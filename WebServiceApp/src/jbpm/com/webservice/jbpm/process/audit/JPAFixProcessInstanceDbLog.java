package com.webservice.jbpm.process.audit;

import java.util.List;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.jbpm.process.audit.JPAProcessInstanceDbLog;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.audit.VariableInstanceLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webservice.jbpm.server.daemon.TaskServerDaemon;

/**
 * <p>Description: [修复jbpm-bam-5.3.0.Final.jar中的JPAProcessInstanceDbLog类]</p>
 * @author  <a href="mailto: swpigris81@126.com">大牙-小白</a>
 * @version v0.1
 */
public class JPAFixProcessInstanceDbLog extends JPAProcessInstanceDbLog {
private static Logger logger = LoggerFactory.getLogger(JPAFixProcessInstanceDbLog.class);
    
    private static volatile Environment env;
    private static EntityManagerFactory emf;
    
    static { 
        try { 
            emf = TaskServerDaemon.jtaEmf;
            if(emf == null){
                throw new Exception("not init entity manager factory, now init....");
            }
        } catch( Exception e ) { 
            try{
                emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
            }catch(Exception e1){
                logger.debug("Unable to instantiate emf for 'org.jbpm.persistence.jpa' persistence unit: " + e1.getMessage() );
            }
        }
    }
    
    @Deprecated
    public JPAFixProcessInstanceDbLog() {
    }
    
    @Deprecated
    public JPAFixProcessInstanceDbLog(Environment env){
        JPAFixProcessInstanceDbLog.env = env;
    }
    
    public static void setEnvironment(Environment newEnv) { 
        env = newEnv;
    }
    
    public void og() { 
        
    }
    
    @SuppressWarnings("unchecked")
    public static List<ProcessInstanceLog> findProcessInstances() {
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        List<ProcessInstanceLog> result = em.createQuery("FROM ProcessInstanceLog").getResultList();
        closeEntityManager(em, ut);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<ProcessInstanceLog> findProcessInstances(String processId) {
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        List<ProcessInstanceLog> result = em
            .createQuery("FROM ProcessInstanceLog p WHERE p.processId = :processId")
                .setParameter("processId", processId).getResultList();
        closeEntityManager(em, ut);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<ProcessInstanceLog> findActiveProcessInstances(String processId) {
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        List<ProcessInstanceLog> result = getEntityManager()
            .createQuery("FROM ProcessInstanceLog p WHERE p.processId = :processId AND p.end is null")
                .setParameter("processId", processId).getResultList();
        closeEntityManager(em, ut);
        return result;
    }

    public static ProcessInstanceLog findProcessInstance(long processInstanceId) {
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        ProcessInstanceLog result = (ProcessInstanceLog) getEntityManager()
            .createQuery("FROM ProcessInstanceLog p WHERE p.processInstanceId = :processInstanceId")
                .setParameter("processInstanceId", processInstanceId).getSingleResult();
        closeEntityManager(em, ut);
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public static List<NodeInstanceLog> findNodeInstances(long processInstanceId) {
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        List<NodeInstanceLog> result = getEntityManager()
            .createQuery("FROM NodeInstanceLog n WHERE n.processInstanceId = :processInstanceId ORDER BY n.date")
                .setParameter("processInstanceId", processInstanceId).getResultList();
        closeEntityManager(em, ut);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<NodeInstanceLog> findNodeInstances(long processInstanceId, String nodeId) {
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        List<NodeInstanceLog> result = getEntityManager()
            .createQuery("FROM NodeInstanceLog n WHERE n.processInstanceId = :processInstanceId AND n.nodeId = :nodeId ORDER BY n.date")
                .setParameter("processInstanceId", processInstanceId)
                .setParameter("nodeId", nodeId).getResultList();
        closeEntityManager(em, ut);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<VariableInstanceLog> findVariableInstances(long processInstanceId) {
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        List<VariableInstanceLog> result = getEntityManager()
            .createQuery("FROM VariableInstanceLog v WHERE v.processInstanceId = :processInstanceId ORDER BY v.date")
                .setParameter("processInstanceId", processInstanceId).getResultList();
        closeEntityManager(em, ut);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<VariableInstanceLog> findVariableInstances(long processInstanceId, String variableId) {
        EntityManager em = getEntityManager();
        UserTransaction ut = joinTransaction(em);
        List<VariableInstanceLog> result = em
            .createQuery("FROM VariableInstanceLog v WHERE v.processInstanceId = :processInstanceId AND v.variableId = :variableId ORDER BY v.date")
                .setParameter("processInstanceId", processInstanceId)
                .setParameter("variableId", variableId).getResultList();
        closeEntityManager(em, ut);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void clear() {
            EntityManager em = getEntityManager();
            UserTransaction ut = joinTransaction(em);
            
            List<ProcessInstanceLog> processInstances = em.createQuery("FROM ProcessInstanceLog").getResultList();
            for (ProcessInstanceLog processInstance: processInstances) {
                em.remove(processInstance);
            }
            List<NodeInstanceLog> nodeInstances = em.createQuery("FROM NodeInstanceLog").getResultList();
            for (NodeInstanceLog nodeInstance: nodeInstances) {
                em.remove(nodeInstance);
            }
            List<VariableInstanceLog> variableInstances = em.createQuery("FROM VariableInstanceLog").getResultList();
            for (VariableInstanceLog variableInstance: variableInstances) {
                em.remove(variableInstance);
            }           
            closeEntityManager(em, ut);
    }

    @Deprecated
    public static void dispose() {
        if (emf != null) {
            emf.close();
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (emf != null) {
            emf.close();
        }
    }

    /**
     * This method opens a new transaction, if none is currently running, and joins the entity manager/persistence context
     * to that transaction. 
     * @param em The entity manager we're using. 
     * @return {@link UserTransaction} If we've started a new transaction, then we return it so that it can be closed. 
     * @throws NotSupportedException 
     * @throws SystemException 
     * @throws Exception if something goes wrong. 
     */
    private static UserTransaction joinTransaction(EntityManager em) {
        boolean newTx = false;
        UserTransaction ut = null;
        try { 
            ut = (UserTransaction) new InitialContext().lookup( "java:comp/UserTransaction" );
            if( ut.getStatus() == Status.STATUS_NO_TRANSACTION ) { 
                ut.begin();
                newTx = true;
            }
        } catch(Exception e) { 
            logger.error("Unable to find or open a transaction: " + e.getMessage());
            e.printStackTrace();
        }
        
        em.joinTransaction(); 
       
        if( newTx ) { 
            return ut;
        }
        return null;
    }

    /**
     * This method closes the entity manager and transaction. It also makes sure that any objects associated 
     * with the entity manager/persistence context are detached. 
     * </p>
     * Obviously, if the transaction returned by the {@link #joinTransaction(EntityManager)} method is null, 
     * nothing is done with the transaction parameter.
     * @param em The entity manager.
     * @param ut The (user) transaction.
     */
    private static void closeEntityManager(EntityManager em, UserTransaction ut) {
        em.flush(); // This saves any changes made
        em.clear(); // This makes sure that any returned entities are no longer attached to this entity manager/persistence context
        em.close(); // and this closes the entity manager
        try { 
            if( ut != null ) { 
                // There's a tx running, close it.
                ut.commit();
            }
        } catch(Exception e) { 
            logger.error("Unable to commit transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method creates a entity manager. If an environment has already been 
     * provided, we use the entity manager factory present in the environment. 
     * </p> 
     * Otherwise, we assume that the persistence unit is called "org.jbpm.persistence.jpa"
     * and use that to build the entity manager factory. 
     * @return an entity manager
     */
    private static EntityManager getEntityManager() {
        EntityManager em = null;
        if (env == null) {
            em = emf.createEntityManager();
        } else {
            EntityManagerFactory emf = (EntityManagerFactory) env.get(EnvironmentName.ENTITY_MANAGER_FACTORY);
            em = emf.createEntityManager(); 
        }
        return em;
    }
}
