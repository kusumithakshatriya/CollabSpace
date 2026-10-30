package com.collabspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import java.util.concurrent.*;

@SpringBootApplication
public class CollabSpaceApplication {
  public static void main(String[] args) { SpringApplication.run(CollabSpaceApplication.class, args); }
}

@Configuration
@EnableWebSocket
class SocketConfig implements WebSocketConfigurer {
  @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(new CollaborationSocket(), "/ws").setAllowedOrigins("*");
  }
}
class CollaborationSocket extends TextWebSocketHandler {
  static final Set<WebSocketSession> clients = new CopyOnWriteArraySet<>();
  @Override public void afterConnectionEstablished(WebSocketSession s) { clients.add(s); }
  @Override protected void handleTextMessage(WebSocketSession s, TextMessage message) {
    for (WebSocketSession client : clients) try { if (client.isOpen()) client.sendMessage(message); } catch (Exception ignored) {}
  }
  @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus status) { clients.remove(s); }
}

@RestController
@RequestMapping("/api")
class ApiController {
  private final Map<String, Map<String,Object>> users = new ConcurrentHashMap<>();
  private final Map<String, String> tokens = new ConcurrentHashMap<>();
  private final List<Map<String,Object>> tasks = new CopyOnWriteArrayList<>();
  private final List<Map<String,Object>> messages = new CopyOnWriteArrayList<>();
  private final List<Map<String,Object>> documents = new CopyOnWriteArrayList<>();
  private final List<Map<String,Object>> whiteboard = new CopyOnWriteArrayList<>();
  private final List<Map<String,Object>> codeSnippets = new CopyOnWriteArrayList<>();
  private final List<Map<String,Object>> activity = new CopyOnWriteArrayList<>();
  private int nextTask=4, nextMessage=4, nextDoc=3, nextShape=1, nextCode=2;

  ApiController() {
    users.put("admin", user("admin","admin@collabspace.local","ADMIN","Alex Morgan"));
    users.put("maya", user("maya","maya@collabspace.local","MEMBER","Maya Chen"));
    users.put("sam", user("sam","sam@collabspace.local","MEMBER","Sam Patel"));
    tasks.add(task(1,"Design workspace dashboard","Maya Chen","IN PROGRESS","High"));
    tasks.add(task(2,"Create authentication API","Sam Patel","TODO","High"));
    tasks.add(task(3,"Write onboarding documentation","Alex Morgan","DONE","Medium"));
    messages.add(message(1,"Alex Morgan","Welcome to CollabSpace! Use this room to coordinate your work."));
    messages.add(message(2,"Maya Chen","I have started the dashboard design. Feedback welcome."));
    messages.add(message(3,"Sam Patel","I'll take the authentication API task."));
    documents.add(doc(1,"Project brief","Goals, scope, milestones, and delivery notes for the team.","Alex Morgan"));
    documents.add(doc(2,"API notes","REST endpoints use JSON. Keep authorization checks on every protected action.","Maya Chen"));
    codeSnippets.add(map("id",1,"title","Welcome example","language","java","code","public class HelloTeam {\n  public static void main(String[] args) {\n    System.out.println(\"Hello, CollabSpace!\");\n  }\n}","updatedBy","Alex Morgan"));
    activity.add(map("id",1,"text","Workspace created","when","Just now"));
  }
  private Map<String,Object> map(Object... pairs) { Map<String,Object> m=new LinkedHashMap<>(); for(int i=0;i<pairs.length;i+=2)m.put(String.valueOf(pairs[i]),pairs[i+1]); return m; }
  private Map<String,Object> user(String u,String email,String role,String name){return map("username",u,"email",email,"role",role,"name",name);}
  private Map<String,Object> task(int id,String title,String assignee,String status,String priority){return map("id",id,"title",title,"assignee",assignee,"status",status,"priority",priority,"createdAt",System.currentTimeMillis());}
  private Map<String,Object> message(int id,String author,String text){return map("id",id,"author",author,"text",text,"time",System.currentTimeMillis());}
  private Map<String,Object> doc(int id,String title,String content,String author){return map("id",id,"title",title,"content",content,"author",author,"updatedAt",System.currentTimeMillis());}
  private Map<String,Object> authUser(String token) { String u=tokens.get(token); return u==null?null:users.get(u); }
  private String display(String token) { Map<String,Object> u=authUser(token); return u==null?"Guest":String.valueOf(u.get("name")); }
  private boolean admin(String token){Map<String,Object> u=authUser(token);return u!=null&&"ADMIN".equals(u.get("role"));}
  private void broadcast(String type,Object data) { try { String payload=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map("type",type,"data",data)); for(WebSocketSession s:CollaborationSocket.clients) if(s.isOpen())s.sendMessage(new TextMessage(payload)); } catch(Exception ignored){} }
  @GetMapping("/health") public Map<String,Object> health(){return map("status","UP","application","CollabSpace");}
  @PostMapping("/login") public ResponseEntity<?> login(@RequestBody Map<String,String> body){
    String username=body.getOrDefault("username","").toLowerCase(), password=body.getOrDefault("password","");
    if(!users.containsKey(username)||!password.equals("collab123"))return ResponseEntity.status(401).body(map("error","Invalid credentials. Use admin, maya, or sam with password collab123."));
    String token=UUID.randomUUID().toString();tokens.put(token,username);Map<String,Object> result=new LinkedHashMap<>(users.get(username));result.put("token",token);return ResponseEntity.ok(result);
  }
  @GetMapping("/me") public ResponseEntity<?> me(@RequestHeader(value="X-Auth-Token",required=false) String token){Map<String,Object> u=authUser(token);return u==null?ResponseEntity.status(401).body(map("error","Please sign in")):ResponseEntity.ok(u);}
  @GetMapping("/dashboard") public Map<String,Object> dashboard(){long done=tasks.stream().filter(t->"DONE".equals(t.get("status"))).count();return map("members",users.size(),"tasks",tasks.size(),"completed",done,"messages",messages.size(),"documents",documents.size(),"activity",activity);}
  @GetMapping("/tasks") public List<Map<String,Object>> getTasks(){return tasks;}
  @PostMapping("/tasks") public ResponseEntity<?> addTask(@RequestHeader(value="X-Auth-Token",required=false) String token,@RequestBody Map<String,String> b){
    if(authUser(token)==null)return ResponseEntity.status(401).body(map("error","Sign in required"));
    Map<String,Object> t=task(nextTask++,b.getOrDefault("title","Untitled task"),b.getOrDefault("assignee",display(token)),b.getOrDefault("status","TODO"),b.getOrDefault("priority","Medium"));tasks.add(t);activity.add(map("id",activity.size()+1,"text",display(token)+" created task: "+t.get("title"),"when","Just now"));broadcast("task",t);return ResponseEntity.ok(t);
  }
  @PatchMapping("/tasks/{id}") public ResponseEntity<?> updateTask(@RequestHeader(value="X-Auth-Token",required=false) String token,@PathVariable int id,@RequestBody Map<String,String> b){
    if(authUser(token)==null)return ResponseEntity.status(401).body(map("error","Sign in required"));
    for(Map<String,Object> t:tasks)if(((Number)t.get("id")).intValue()==id){if(b.containsKey("status"))t.put("status",b.get("status"));if(b.containsKey("assignee"))t.put("assignee",b.get("assignee"));broadcast("task",t);return ResponseEntity.ok(t);}return ResponseEntity.notFound().build();
  }
  @GetMapping("/messages") public List<Map<String,Object>> getMessages(){return messages;}
  @PostMapping("/messages") public ResponseEntity<?> addMessage(@RequestHeader(value="X-Auth-Token",required=false) String token,@RequestBody Map<String,String> b){
    if(authUser(token)==null)return ResponseEntity.status(401).body(map("error","Sign in required"));String txt=b.getOrDefault("text","").trim();if(txt.isEmpty())return ResponseEntity.badRequest().body(map("error","Message cannot be empty"));
    Map<String,Object> m=message(nextMessage++,display(token),txt);messages.add(m);broadcast("message",m);return ResponseEntity.ok(m);
  }
  @GetMapping("/documents") public List<Map<String,Object>> getDocuments(){return documents;}
  @PostMapping("/documents") public ResponseEntity<?> addDocument(@RequestHeader(value="X-Auth-Token",required=false) String token,@RequestBody Map<String,String> b){
    if(authUser(token)==null)return ResponseEntity.status(401).body(map("error","Sign in required"));Map<String,Object>d=doc(nextDoc++,b.getOrDefault("title","Untitled"),b.getOrDefault("content",""),display(token));documents.add(d);broadcast("document",d);return ResponseEntity.ok(d);
  }
  @GetMapping("/whiteboard") public List<Map<String,Object>> getWhiteboard(){return whiteboard;}
  @PostMapping("/whiteboard") public ResponseEntity<?> addShape(@RequestHeader(value="X-Auth-Token",required=false) String token,@RequestBody Map<String,Object> b){
    if(authUser(token)==null)return ResponseEntity.status(401).body(map("error","Sign in required"));Map<String,Object>s=map("id",nextShape++,"x",b.getOrDefault("x",80),"y",b.getOrDefault("y",80),"text",b.getOrDefault("text","Idea"),"color",b.getOrDefault("color","#dbeafe"),"author",display(token));whiteboard.add(s);broadcast("shape",s);return ResponseEntity.ok(s);
  }
  @GetMapping("/code") public List<Map<String,Object>> getCode(){return codeSnippets;}
  @PostMapping("/code") public ResponseEntity<?> saveCode(@RequestHeader(value="X-Auth-Token",required=false) String token,@RequestBody Map<String,String> b){
    if(authUser(token)==null)return ResponseEntity.status(401).body(map("error","Sign in required"));Map<String,Object> c=map("id",nextCode++,"title",b.getOrDefault("title","Untitled snippet"),"language",b.getOrDefault("language","text"),"code",b.getOrDefault("code",""),"updatedBy",display(token));codeSnippets.add(c);broadcast("code",c);return ResponseEntity.ok(c);
  }
  @PostMapping("/assistant") public ResponseEntity<?> assistant(@RequestHeader(value="X-Auth-Token",required=false) String token,@RequestBody Map<String,String> b){
    if(authUser(token)==null)return ResponseEntity.status(401).body(map("error","Sign in required"));String q=b.getOrDefault("prompt","").toLowerCase();String answer;
    if(q.contains("summar")||q.contains("meeting"))answer="Workspace summary: "+tasks.size()+" tasks ("+tasks.stream().filter(t->"DONE".equals(t.get("status"))).count()+" completed), "+messages.size()+" chat messages, and "+documents.size()+" shared documents. Current priorities include: "+tasks.stream().filter(t->!"DONE".equals(t.get("status"))).map(t->String.valueOf(t.get("title"))).limit(3).reduce((a,c)->a+"; "+c).orElse("none")+".";
    else if(q.contains("bug")||q.contains("error")||q.contains("debug"))answer="Debugging checklist: reproduce the issue, read the full stack trace, inspect recent changes, validate inputs and null handling, add a focused test, then verify the fix. Paste the relevant code and error message for a more specific review.";
    else if(q.contains("explain")||q.contains("code"))answer="Code review guidance: identify the inputs and outputs, trace the main control flow, check boundary cases and exceptions, and add tests for expected and invalid inputs. Paste a code snippet to get a line-by-line explanation.";
    else answer="I can help with code explanations, debugging checklists, and project summaries. Try: “Summarize our workspace”, “How do I debug a null pointer error?”, or paste code and ask “Explain this code”. This demo assistant runs locally; connect an AI provider to enable model-generated answers.";
    return ResponseEntity.ok(map("answer",answer,"mode","local-demo"));
  }
}
