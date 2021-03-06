package com.radiadesign.catalina.session;

import java.security.Principal;
import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import java.util.HashMap;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;


public class RedisSession extends StandardSession {
  private static Logger log = Logger.getLogger("RedisSession");

  protected static Boolean manualDirtyTrackingSupportEnabled = false;

  public void setManualDirtyTrackingSupportEnabled(Boolean enabled) {
    manualDirtyTrackingSupportEnabled = enabled;
  }

  protected static String manualDirtyTrackingAttributeKey = "__changed__";

  public void setManualDirtyTrackingAttributeKey(String key) {
    manualDirtyTrackingAttributeKey = key;
  }


  protected HashMap<String, Object> changedAttributes;
  protected Boolean dirty;

  public RedisSession(Manager manager) {
    super(manager);
    resetDirtyTracking();
  }

  public Boolean isDirty() {
    return dirty || !changedAttributes.isEmpty();
  }

  public HashMap<String, Object> getChangedAttributes() {
    return changedAttributes;
  }

  public void resetDirtyTracking() {
    changedAttributes = new HashMap<String, Object>();
    dirty = false;
  }

  public void setAttribute(String key, Object value) {
    //set dirty true to solve the setAttribute method does't call after request 
    dirty = true;

    super.setAttribute(key, value);
        
    //flush session into redis
    RedisSessionManager redisManager = (RedisSessionManager) this.manager;
    try {
	redisManager.save(this);
    } catch (IOException e) {
	log.log(Level.SEVERE, "redis session manager save session:[" + this.getId()
		  + "] error:", e);
    }
  }

  public void removeAttribute(String name) {
    dirty = true;
    super.removeAttribute(name);
  }

  @Override
  public void setId(String id) {
    // Specifically do not call super(): it's implementation does unexpected things
    // like calling manager.remove(session.id) and manager.add(session).
    
    this.id = id;
  }

  public void setPrincipal(Principal principal) {
    dirty = true;
    super.setPrincipal(principal);
  }

}
