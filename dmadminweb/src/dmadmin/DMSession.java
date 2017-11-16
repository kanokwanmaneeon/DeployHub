/*
 *
 *  DeployHub is an Agile Application Release Automation Solution
 *  Copyright (C) 2017 Catalyst Systems Corporation DBA OpenMake Software
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dmadmin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.postgresql.largeobject.LargeObject;
import org.postgresql.largeobject.LargeObjectManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.xml.internal.ws.org.objectweb.asm.Type;
import dmadmin.json.CreatedModifiedField;
import dmadmin.json.JSONArray;
import dmadmin.json.JSONBoolean;
import dmadmin.json.JSONObject;
import dmadmin.model.Action;
import dmadmin.model.Action.ActionArg;
import dmadmin.model.Action.SwitchMode;
import dmadmin.model.ActionKind;
import dmadmin.model.ActionParameter;
import dmadmin.model.Application;
import dmadmin.model.Attachment;
import dmadmin.model.BuildJob;
import dmadmin.model.Builder;
import dmadmin.model.Category;
import dmadmin.model.CompType;
import dmadmin.model.Component;
import dmadmin.model.ComponentFilter;
import dmadmin.model.ComponentItem;
import dmadmin.model.ComponentLink;
import dmadmin.model.Credential;
import dmadmin.model.CredentialKind;
import dmadmin.model.DMAttribute;
import dmadmin.model.DMCalendarEvent;
import dmadmin.model.DMObject;
import dmadmin.model.DMProperty;
import dmadmin.model.DMPropertyDef;
import dmadmin.model.Datasource;
import dmadmin.model.DeployDepsEdge;
import dmadmin.model.DeployDepsNode;
import dmadmin.model.DeployedApplication;
import dmadmin.model.Deployment;
import dmadmin.model.Domain;
import dmadmin.model.Engine;
import dmadmin.model.Environment;
import dmadmin.model.Fragment;
import dmadmin.model.FragmentAttributes;
import dmadmin.model.FragmentDetails;
import dmadmin.model.FragmentListValues;
import dmadmin.model.IPrePostAction;
import dmadmin.model.LoginException;
import dmadmin.model.LoginException.LoginExceptionType;
import dmadmin.model.Notify;
import dmadmin.model.NotifyTemplate;
import dmadmin.model.ObjectAccess;
import dmadmin.model.Plugin;
import dmadmin.model.ProviderDefinition;
import dmadmin.model.ProviderObject;
import dmadmin.model.Repository;
import dmadmin.model.Server;
import dmadmin.model.ServerLink;
import dmadmin.model.ServerStatus;
import dmadmin.model.ServerType;
import dmadmin.model.ServerType.LineEndFormat;
import dmadmin.model.Task;
import dmadmin.model.Task.TaskType;
import dmadmin.model.TaskAction;
import dmadmin.model.TaskApprove;
import dmadmin.model.TaskAudit;
import dmadmin.model.TaskCreateVersion;
import dmadmin.model.TaskDeploy;
import dmadmin.model.TaskList;
import dmadmin.model.TaskMove;
import dmadmin.model.TaskParameter;
import dmadmin.model.TaskRemove;
import dmadmin.model.TaskRequest;
import dmadmin.model.TaskUserDefined;
import dmadmin.model.Transfer;
import dmadmin.model.TreeObject;
import dmadmin.model.User;
import dmadmin.model.UserGroup;
import dmadmin.model.UserGroupList;
import dmadmin.model.UserList;
import dmadmin.model.UserPermissions;
import dmadmin.util.CommandLine;
import dmadmin.util.DynamicQueryBuilder;
import dmadmin.util.DynamicQueryBuilder.Null;

public class DMSession {
	
	private String m_defaultdatefmt = "MM/dd/yyyy";
	private String m_defaulttimefmt = "HH:mm";
	
	private Map<Integer, String> m_domains;
	private Connection m_conn;
	private String m_domainlist;
	private String m_parentdomains;
	private HttpSession m_httpSession;
    private ServletContext m_context;
	
	// Copy/Paste
	private String m_copyobjtype;
	private int m_copyid;
	
	private int m_userDomain;
	private int m_userID;
	private boolean m_newUser;
	private String m_datefmt;
	private String m_timefmt;
	private String m_username;
	private String m_password;

	private boolean m_OverrideAccessControl;
	private boolean m_EndPointsTab;
	private boolean m_ApplicationsTab;
	private boolean m_ActionsTab;
	private boolean m_ProvidersTab;
	private boolean m_UsersTab;	
	private UserPermissions m_UserPermissions;
	private String AssociatedMsg;
	private String dbdriver;
	private String whencol;				// Column name of "when"
	private String sizecol;				// Column name of "size" (keyword in Oracle)
	private String m_PasteError;
	String username = "postgres";
	String password = "postgres";
	
	// For background syncing of defects
	private Hashtable<Integer,int[]> m_pollhash = null;
	
	// For caching objects that we request over and over
	Hashtable<Integer,Domain> m_domainhash = null;
	long m_domainhashCreationTime = 0;
	Hashtable<Integer,User> m_userhash = null;
	long m_userhashCreationTime = 0;
	Hashtable <Integer,List<Domain>> m_cdhash = null;
	long m_cdhashCreationTime = 0;
	
	byte[] m_passphrase;			// Moved to global to allow partial credential decryption
	
	private String m_webhostname;			// set from request during login.
	
	public static final int HOME_TAB_WORKBENCH 						= 1;	
	public static final int HOME_TAB_ENDPOINTS_AND_CREDENTIALS 		= 2;
	public static final int HOME_TAB_APPLICATIONS_AND_COMPONENTS	= 3;
	public static final int HOME_TAB_ACTIONS_AND_PROCEDURES         = 4;
	public static final int HOME_TAB_PROVIDERS 						= 5;
	public static final int HOME_TAB_USERS_AND_GROUPS 				= 6;

	public static final int EXPLORER_TAB_WORKBENCH_WORKFLOW 						= 11;
	public static final int EXPLORER_TAB_WORKBENCH_ENVIRONMENTS                 	= 12;
	
	public static final int EXPLORER_TAB_ENDPOINTS_AND_CREDENTIALS_ENDPOINTS 		= 21;
	public static final int EXPLORER_TAB_ENDPOINTS_AND_CREDENTIALS_SERVERS	 		= 22;
	public static final int EXPLORER_TAB_ENDPOINTS_AND_CREDENTIALS_CREDENTIALS 		= 23;
	
	public static final int EXPLORER_TAB_APPLICATIONS_AND_COMPONENTS_APPLICATIONS	= 31;
	public static final int EXPLORER_TAB_APPLICATIONS_AND_COMPONENTS_COMPONENTS 	= 32;
	
	public static final int EXPLORER_TAB_ACTIONS_AND_PROCEDURES_ACTIONS 			= 41;
	public static final int EXPLORER_TAB_ACTIONS_AND_PROCEDURES_PROCEDURES 			= 42;
	public static final int EXPLORER_TAB_ACTIONS_AND_PROCEDURES_FUNCTIONS 			= 43;
	
	public static final int EXPLORER_TAB_PROVIDERS_DATASOURCES			 			= 51;
	public static final int EXPLORER_TAB_PROVIDERS_NOTIFIERS			 			= 52;
	
	public static final int EXPLORER_TAB_USERS_AND_GROUPS_USERS			 			= 61;
	public static final int EXPLORER_TAB_USERS_AND_GROUPS_GROUPS		 			= 62;
	
	// For finding fully qualified domains
	public static final int DOMAIN_NOT_FOUND = -1;
	public static final int DOMAIN_NOT_SPECIFIED = -2;
	public static final int DOMAIN_OBJECT_AMBIGUOUS = -3;
	
	// mutex locks for GetID()
	private Object mutex = null;
	
	private void initSession(ServletContext context)
	{
		m_domains = new HashMap<Integer, String>();
		m_userDomain = 0;
		setUserID(0);
		m_domainlist="";
		m_parentdomains="";	
		dbdriver = context.getInitParameter("DBDriverName");
		if (dbdriver.toLowerCase().contains("oracle")) {
			whencol = "\"WHEN\"";	// Quoted name must be upper case for Oracle
			sizecol = "\"SIZE\"";	// size is keyword in Oracle and must be quoted.
		} else {
			whencol = "\"WHEN\"";	// Installer creates when as upper case
			sizecol = "size";		// size is not a keyword in Postgres
		}
	}
	
	public DMSession(ServletContext context)
	{
		initSession(context);
		m_context = context;
	}
	
	public DMSession(HttpSession session)
	{
		initSession(session.getServletContext());
		m_httpSession = session;
		m_context = session.getServletContext();
	}
	
	

	public String GetSessionId()
	{
		return m_httpSession.getId();
	}
	
	public Connection GetConnection()
	{
		return getDBConnection();
	}
	
	public int GetUserID()
	{
		return getUserID();
	}
	
	public String getNewUser()
	{
		return m_newUser?"Y":"N";
	}
	
	public void setWebHostName(HttpServletRequest request)
	{
		
		String hn = request.getServerName();
		System.out.println("server name frm request="+hn);
		if (hn.equalsIgnoreCase("localhost")) {
			try {
				InetAddress ip = InetAddress.getLocalHost();
	            hn = ip.getHostName();
	            System.out.println("server name from getLocalHost="+hn);
			} catch (UnknownHostException ex) {
				// If our local host is unknown, not sure what we do!
				hn="localhost";
			}
		} 
		m_webhostname = hn;
	}
	
	public String getWebHostName()
	{
		return m_webhostname;
	}

	public long timeNow()
	{
		return (long)(System.currentTimeMillis()/1000);
	}
	
	public boolean getAclOverride() {
		return m_OverrideAccessControl;
	}
	
	public boolean getEndPointsTabAccess() {
		return m_EndPointsTab;
	}
	
	public boolean getApplicationsTabAccess() {
		return m_ApplicationsTab;
	}
	
	public boolean getActionsTabAccess() {
		return m_ActionsTab;
	}
	
	public boolean getProvidersTabAccess() {
		return m_ProvidersTab;
	}
	
	public boolean getUsersTabAccess() {
		return m_UsersTab;
	}
	
	public void updateModTime(DMObject obj)
	{
		// Generic update to the object's underlying table.
		String tabname = obj.getDatabaseTable();
		int id = obj.getId();
		long t = timeNow();
		String sql = "UPDATE "+tabname+" SET modified=?, modifierid=? WHERE id=?";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setLong(1,t);
			stmt.setInt(2,getUserID());
			stmt.setInt(3,id);
			stmt.execute();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
	}
	
	private void GetSubDomains(Map<Integer,String>domainlist,Integer DomainID)
	{   
		 try
		 {
			 	Statement st = getDBConnection().createStatement();
				ResultSet rs = st.executeQuery("SELECT id FROM dm.dm_domain where domainid = "+DomainID);
				while (rs.next())
				{
					domainlist.put(rs.getInt(1),"Y");
					//
					// Recurse
					//
					GetSubDomains(domainlist,rs.getInt(1));
				}
				rs.close();
				st.close();
	     }
	     catch (Exception e)
	     {
	         //out.println("An exception occurred: " + e.getMessage());
	     }
	}

	private void GetParentDomains(Integer DomainID)
	{   
		 try
		 {		
			 	Statement st = getDBConnection().createStatement();
				ResultSet rs = st.executeQuery("SELECT domainid FROM dm.dm_domain WHERE id= "+DomainID);
				rs.next();
				int parentid = getInteger(rs, 1, 0);
				System.out.println("parentid="+parentid);
				if (parentid != 0)
				{
					m_domains.put(parentid,"N");
					//
					// Recurse
					//
					GetParentDomains(parentid);
				}
				rs.close();
				st.close();
	     }
	     catch (Exception e)
	     {
	    	 rollback();
	    	 e.printStackTrace();
	     }
	}
	
	private void GetDomains(int UserID)
	{
		try
		{
			synchronized(this) {
				m_domains = new HashMap<Integer,String>();
				m_domainlist="";
				m_parentdomains="";
				Statement st = getDBConnection().createStatement();
				ResultSet rs = st.executeQuery("SELECT domainid FROM dm.dm_user where id = "+UserID);
				rs.next();
				m_userDomain = rs.getInt(1);
				System.out.println("user domain = " + m_userDomain);
				rs.close();
				st.close();
				//
				// Okay, now derive a list of all our sub-domains
				//
				System.out.println("Before GetParentDomains, m_domains.size()="+m_domains.size());
				GetParentDomains(m_userDomain);
				System.out.println("After GetParentDomains, m_domains.size()="+m_domains.size());
				Iterator<Map.Entry<Integer,String>> it = m_domains.entrySet().iterator();
				String sep="";
				while (it.hasNext())
				{
				    Map.Entry<Integer,String> pairs = it.next();
				    m_parentdomains=m_parentdomains+sep+pairs.getKey();
				    sep=",";
				}
				System.out.println("m_parentdomains="+m_parentdomains);
				m_domains.put(m_userDomain, "Y");
				
				GetSubDomains(m_domains,m_userDomain);
				
				//
				// Create a "domainlist" string for queries
				//
				it = m_domains.entrySet().iterator();
				sep="";
				while (it.hasNext())
				{
				    Map.Entry<Integer,String> pairs = it.next();
				    m_domainlist=m_domainlist+sep+pairs.getKey();
				    sep=",";
				}
				System.out.println("domainlist="+m_domainlist);
			}
		}
		 catch (Exception e)
	     {
			 rollback();
			 e.printStackTrace();
	     }
	}
	
	private static String encryptPassword(String algorithm,String password)
	{
		String res = "";
	    try
	    {
	        MessageDigest crypt = MessageDigest.getInstance(algorithm);
	        crypt.reset();
	        crypt.update(password.getBytes());
	        res = javax.xml.bind.DatatypeConverter.printBase64Binary(crypt.digest());
	    }
	    catch(NoSuchAlgorithmException e)
	    {
	        e.printStackTrace();
	    }
	    return res;
	}
	
	private static String encryptPassword(String password)
	{
		return encryptPassword("SHA-256",password);
	}
	
	public int UserBaseDomain()
	{
		return m_userDomain;
	}
	
	public Domain getUserDomain()
	{
		return getDomain(m_userDomain);
	}
	
	public UserPermissions getUserPermissions()
	{
		return m_UserPermissions;
	}
	
	private static byte[][] EVP_BytesToKey(int key_len, int iv_len, MessageDigest md, byte[] salt, byte[] data, int count)
	{
        byte[][] both = new byte[2][];
        byte[] key = new byte[key_len];
        int key_ix = 0;
        byte[] iv = new byte[iv_len];
        int iv_ix = 0;
        both[0] = key;
        both[1] = iv;
        byte[] md_buf = null;
        int nkey = key_len;
        int niv = iv_len;
        int i = 0;
        if (data == null) return both;
        int addmd = 0;
        for (;;) {
            md.reset();
            if (addmd++ > 0) md.update(md_buf);
            md.update(data);
            if (null != salt) md.update(salt, 0, 8);
            md_buf = md.digest();
            for (i = 1; i < count; i++) {
                md.reset();
                md.update(md_buf);
                md_buf = md.digest();
            }
            i = 0;
            if (nkey > 0) {
                for (;;) {
                    if (nkey == 0) break;
                    if (i == md_buf.length) break;
                    key[key_ix++] = md_buf[i];
                    nkey--;
                    i++;
                }
            }
            if (niv > 0 && i != md_buf.length) {
                for (;;) {
                    if (niv == 0) break;
                    if (i == md_buf.length) break;
                    iv[iv_ix++] = md_buf[i];
                    niv--;
                    i++;
                }
            }
            if (nkey == 0 && niv == 0) break;
        }
        for (i = 0; i < md_buf.length; i++) md_buf[i] = 0;
        return both;
    }
	
	private byte[] Decrypt3DES(String encodedString,byte[] passphrase)
	{
		try
		{
		    byte[] pp1 = javax.xml.bind.DatatypeConverter.parseBase64Binary(encodedString);
		    byte[] passphrase1 = new byte[pp1.length-16]; // take the "Salted__saltsalt" off the front
		    byte[] salt = new byte[8];
		    for (int n=8, p=0; n < pp1.length;) {
		    	if (p<8) salt[p] = pp1[n];
		    	if (p>=8) passphrase1[p-8] = pp1[n];
		    	p++;
		    	n++;
		    }
		    final Cipher decipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
		    final MessageDigest md = MessageDigest.getInstance("md5");
	        final byte[][] keyAndIV = EVP_BytesToKey(
	        		24,							// Key len
	                decipher.getBlockSize(),	// IV len
	                md,
	                salt,
	                passphrase,
	                1);
	        SecretKeySpec key = new SecretKeySpec(keyAndIV[0], "DESede");
	        IvParameterSpec iv = new IvParameterSpec(keyAndIV[1]);
		    decipher.init(Cipher.DECRYPT_MODE, key, iv);
		    return decipher.doFinal(passphrase1);
		}
		catch (NoSuchAlgorithmException e)
		{
			System.out.println("NO SUCH ALGORITHM EXCEPTION");
			e.printStackTrace();
		}
		catch (NoSuchPaddingException e) {
			System.out.println("NO SUCH PADDING EXCEPTION");
			e.printStackTrace();
		}
		catch (InvalidKeyException e) {
			System.out.println("INVALID KEY EXCEPTION");
			e.printStackTrace();
		}
		catch (InvalidAlgorithmParameterException e) {
			System.out.println("INVALID ALGORITHM PARAMETER EXCEPTION");
			e.printStackTrace();
		}
		catch (IllegalBlockSizeException e) {
			System.out.println("ILLEGAL BLOCK SIZE EXCEPTION");
			e.printStackTrace();
		}
		catch (BadPaddingException e) {
			System.out.println("BAD PADDING EXCEPTION");
			e.printStackTrace();
		}
		return null;
	}
	
	String readFile(String filename) throws FileNotFoundException, IOException
	{
		File file = new File(filename);

			FileInputStream fin = new FileInputStream(file);
			byte fileContent[] = new byte[(int)file.length()];
			fin.read(fileContent); 
			String strFileContent = new String(fileContent);
			fin.close();
			return strFileContent;
	}
	
	private LoginException connectToDatabase(ServletContext context)
	{
		LoginException res = null;
		//
		// Connect to the database
		//
		System.out.println("connectToDatabase()");
		String DMHome = context.getInitParameter("DMHOME");
		String ConnectionString = context.getInitParameter("DBConnectionString");
		String DriverName = context.getInitParameter("DBDriverName");
		System.out.println("DMHOME="+DMHome);
		try {
			String base64Original   = readFile(DMHome+"/dm.odbc");
			String base64passphrase = readFile(DMHome+"/dm.asc");
		   
			m_passphrase = Decrypt3DES(base64passphrase,"dm15k1ng".getBytes("UTF-8"));
		    final byte[] plainText = Decrypt3DES(base64Original,m_passphrase);
		    
		    StringBuilder dDSN = new StringBuilder();
		    StringBuilder dUserName = new StringBuilder();
		    StringBuilder dPassword = new StringBuilder();
		    for (int i=0,d=0;i<plainText.length;i++) {
		    	if (plainText[i]!=0) {
		    		if (d==0) dDSN.append(String.format("%c",plainText[i]));
		    		if (d==1) dUserName.append(String.format("%c",plainText[i]));
		    		if (d==2) dPassword.append(String.format("%c",plainText[i]));
		    	} else d++;
		    }
		    // DSN is ignored for Postgres Driver
			Class.forName(DriverName);
		
			System.out.println("DMHOME=" + DMHome);
			System.out.println("DRIVERNAME=" + DriverName);
			System.out.println("CONNECTIONSTRING=" + ConnectionString);
			System.out.println("USERNAME=" + dUserName.toString());
			System.out.println("PASSWORDNAME=" + dPassword.toString());
			
			setDBConnection(DriverManager.getConnection(ConnectionString,dUserName.toString(),dPassword.toString()));
			getDBConnection().setAutoCommit(false);
		} catch (FileNotFoundException e) {
			res = new LoginException(LoginExceptionType.LOGIN_DATABASE_FAILURE,e.getMessage());
			e.printStackTrace();
	    } catch (IOException e) {
			res = new LoginException(LoginExceptionType.LOGIN_DATABASE_FAILURE,e.getMessage());
			e.printStackTrace();
	    } catch (ClassNotFoundException e) {
			res = new LoginException(LoginExceptionType.LOGIN_DATABASE_FAILURE,"Class not found for " + e.getMessage());
			e.printStackTrace();
		} catch (SQLException e) {
			res = new LoginException(LoginExceptionType.LOGIN_DATABASE_FAILURE,"SQL Exception " + e.getMessage());
			rollback();
		}
		return res;
	}
	
	public LoginException Login(String UserName,String Password)
	{
		LoginException res;
		try
		{
			res = connectToDatabase(m_httpSession.getServletContext());
			if (res != null) throw new LoginException(res.getExceptionType(),res.getMessage());
			if (m_username != null && UserName.equals(m_username)) {
				System.out.println("Already logged in, returning success");
				return new LoginException(LoginExceptionType.LOGIN_OKAY,"");
			}
			PreparedStatement st = m_conn.prepareStatement("SELECT id,passhash,locked,forcechange,datefmt,timefmt,datasourceid,lastlogin FROM dm.dm_user where name = ? and status='N'");	 
			st.setString(1,UserName);
			ResultSet rs = st.executeQuery();
			if (!rs.next()) throw new LoginException(LoginExceptionType.LOGIN_BAD_PASSWORD,"");	// No row retrieved
			//
			// User exists
			// -----------
			//
			// Encrypt the passed password (SHA-256)
			//
			String base64pw = encryptPassword(Password);
			//
			// Compare the encrypted passwords
			//
			String hash = rs.getString(2);
			if((hash == null) || (!base64pw.equals(hash))) throw new LoginException(LoginExceptionType.LOGIN_BAD_PASSWORD,"");
			//
			// Passwords match
			//
			boolean locked = getBoolean(rs,3,false);
			if (locked) throw new LoginException(LoginExceptionType.LOGIN_USER_LOCKED,"");
			boolean forcechange = getBoolean(rs,4,false);
			m_datefmt = rs.getString(5);
			if (rs.wasNull()) m_datefmt = m_defaultdatefmt;
			m_timefmt = rs.getString(6);
			if (rs.wasNull()) m_timefmt = m_defaulttimefmt;
			setUserID(rs.getInt(1));
			rs.getTimestamp(8);
			m_newUser = (rs.wasNull());
			m_OverrideAccessControl = false;
			m_UserPermissions = new UserPermissions(this,0);
			m_password = Password;
			m_username = UserName;
			GetDomains(getUserID());
			PreparedStatement st2 = getDBConnection().prepareStatement("SELECT g.acloverride,g.tabendpoints,g.tabapplications,g.tabactions,g.tabproviders,g.tabusers,g.id FROM dm.dm_usergroup g,dm.dm_usersingroup x WHERE x.userid=? AND g.id=x.groupid");
			st2.setInt(1, getUserID());
			ResultSet rs2 = st2.executeQuery();
			while (rs2.next()) {
				// Loop through each group to which this user belongs
				if (getBoolean(rs2,1,false)) { m_OverrideAccessControl = true; System.out.println("0) User is SUPERUSER"); }
				if (getBoolean(rs2,2,false)) m_EndPointsTab = true;	
				if (getBoolean(rs2,3,false)) m_ApplicationsTab = true;
				if (getBoolean(rs2,4,false)) m_ActionsTab = true;
				if (getBoolean(rs2,5,false)) m_ProvidersTab = true;	
				if (getBoolean(rs2,6,false)) m_UsersTab = true;
				if (m_OverrideAccessControl) {
					m_UserPermissions.setCreateUsers(true);
					m_UserPermissions.setCreateGroups(true);
					m_UserPermissions.setCreateDomains(true);
					m_UserPermissions.setCreateEnvs(true);
					m_UserPermissions.setCreateServers(true);
					m_UserPermissions.setCreateRepos(true);
					m_UserPermissions.setCreateComps(true);
					m_UserPermissions.setCreateCreds(true);
					m_UserPermissions.setCreateApps(true);
					m_UserPermissions.setCreateAppvers(true);
					m_UserPermissions.setCreateActions(true);
					m_UserPermissions.setCreateProcs(true);
					m_UserPermissions.setCreateDatasrc(true);
					m_UserPermissions.setCreateNotifiers(true);
					m_UserPermissions.setCreateEngines(true);
				} else {
					setUserPermissions(rs2.getInt(7),m_UserPermissions);
				}
			}
			rs2.close();
			st2.close();
			if (forcechange) {
				res = new LoginException(LoginExceptionType.LOGIN_CHANGE_PASSWORD,"");
			} else {
				// Login okay - update last login
				String ullsql = 
				(dbdriver.toLowerCase().contains("oracle"))?
				"UPDATE dm.dm_user SET lastlogin = sysdate WHERE id=?":
				"UPDATE dm.dm_user SET lastlogin = localtimestamp WHERE id=?";	
				System.out.println("ullsql="+ullsql);
				PreparedStatement ull = getDBConnection().prepareStatement(ullsql);
				ull.setInt(1, getUserID());
				ull.execute();
				getDBConnection().commit();
				System.out.println("commited");
				res = new LoginException(LoginExceptionType.LOGIN_OKAY,"");
			}
			rs.close();
			st.close();
			System.out.println("User "+getUserID()+" logged in, domains="+m_domainlist);
		}
		catch (SQLException e)
		{
			res = new LoginException(LoginExceptionType.LOGIN_DATABASE_FAILURE,"SQL Exception " + e.getMessage());
			rollback();
		}
		catch (LoginException e)
		{
			res = e;
		}
		return res;
	}
	
	public LoginException InitialLogin(String password)
	{
		// We've been called to do an initial password set for the admin user followed
		// by a login. As a security precaution, we only set the password if the lastlogin
		// field for id 1 is null (which it should only be on initial setup). This prevents
		// someone hacking the admin password by doing a login call with initial=Y
		//
		System.out.println("InitialLogin("+password+")");
		if (firstInstall().equalsIgnoreCase("y")) {
			System.out.println("Resetting admin password");
			// set the admin password to the specified value before logging in
			System.out.println("Setting admin password for InitialLogin");
			String base64pw = encryptPassword(password);
			try {
				PreparedStatement stmt = m_conn.prepareStatement("UPDATE dm.dm_user SET passhash=? WHERE id=1");
				stmt.setString(1,base64pw);
				stmt.execute();
				stmt.close();
				getDBConnection().commit();
			} catch (SQLException ex) {
				System.out.println("Setting initial admin password throws SQLException:"+ex.getMessage());
			}
			
		} else {
			System.out.println("First install is N");
		}
		System.out.println("logging in with admin/"+password);
		return Login("admin",password);
	}
	
	public String getPassword()
	{
		return m_password;
	}
	
	public boolean ValidDomain(int DomainID,Boolean Inherit)
	{
		if (DomainID<=0) return true;	// Any Domain ID 0 or below is considered valid
		
		boolean res = false;
		Iterator<Map.Entry<Integer,String>> it = m_domains.entrySet().iterator();
		while (it.hasNext())
		{
		    Map.Entry<Integer,String> pairs = it.next();
		    if (pairs.getKey() == DomainID)
		    {
		    	// Value is Y if this is our domain or a sub-domain, N if it is a parent domain
		    	if (Inherit)
		    	{
		    		// regardless of value, this is a valid domain (it's either inherited or a sub-domain)
		    		res = true;
		    	}
		    	else
		    	{
		    		// It's only valid if the value is Y
		    		String kv = (String)pairs.getValue();
		    		if (kv.charAt(0) == 'Y')
		    		{
		    			res = true;
		    		}
		    		else
		    		{
		    			res = false;
		    		}
		    	}
		    	break;	// found the domain
		    }
		}
		return res;
	}
	
	public boolean ValidDomain(int DomainID)
	{
		return ValidDomain(DomainID,false);
	}
	
	public int getID(String objectType)
	{
		int res = 0;
		boolean KeyFound=false;
		objectType = objectType.toLowerCase();
		
		if (objectType.equalsIgnoreCase("release") || objectType.equalsIgnoreCase("appversion"))
		 objectType = "application";
		
		if (objectType.equalsIgnoreCase("function") || objectType.equalsIgnoreCase("procedure"))
		 objectType = "action";
		
		if (objectType.equalsIgnoreCase("builder"))
		 objectType = "buildengine";
		
		if (mutex == null) mutex = new Object();
		
		synchronized (mutex) {
			try {
				PreparedStatement st = getDBConnection().prepareStatement("SELECT id FROM dm.dm_keys WHERE lower(object)=? FOR UPDATE");
				st.setString(1, objectType);
				ResultSet rs = st.executeQuery();
				if (rs.next())
				{
					KeyFound=true;
					res = rs.getInt(1);
					System.out.println("Key found, res="+res);
					
					Statement st4 = getDBConnection().createStatement();
					ResultSet rs4 = st4.executeQuery("SELECT coalesce(max(id),0) FROM dm.dm_"+objectType);
					int key2 = 0;
					if(rs4.next()) {
						key2 = rs4.getInt(1);
					} 
					rs4.close();
					if (key2 > res && key2 > 0) res = key2;	// only use the max val if it's greater than the value in dm_keys
					res++;					// next key
					PreparedStatement st3 = getDBConnection().prepareStatement("UPDATE dm.dm_keys SET id=? WHERE lower(object)=?");
					st3.setInt(1,res);
					st3.setString(2, objectType);
					st3.execute();
					st3.close();
				}
				else
				{
					KeyFound=false;
				}
				rs.close();
				st.close();
				
				if (!KeyFound)
				{
					// key does not exist - create it
					PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_keys(object,id) VALUES (?,?)");
					st2.setString(1, objectType);
					
					Statement st3 = getDBConnection().createStatement();
					ResultSet rs3 = st3.executeQuery("SELECT coalesce(max(id),0)+1 FROM dm.dm_"+objectType);
					if(rs3.next()) {
						res = rs3.getInt(1);	// return the new value NOT 0!!!
						System.out.println("Key not found, inserting ('"+objectType+"',"+res+")");
					} else {
						res = 1;
						System.out.println("Key not found, max failed, inserting ('"+objectType+"',"+res+")");
					}
					st2.setInt(2, res);
					rs3.close();
					st2.execute();
					st2.close();
				}
				rs.close();
				// m_conn.commit();	// we probably don't want to commit here.
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				rollback();
			}
		}
		return res;
	}
	
	public int setID(String ObjectType,int newval)
	{
		int res = 0;
		boolean KeyFound=false;
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("SELECT id FROM dm.dm_keys WHERE object=? FOR UPDATE");
			st.setString(1,ObjectType);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				KeyFound=true;
				PreparedStatement st3 = getDBConnection().prepareStatement("UPDATE dm.dm_keys SET id=? WHERE object=?");
				st3.setInt(1,newval);
				st3.setString(2, ObjectType);
				st3.execute();
				st3.close();
			}
			else
			{
				KeyFound=false;
			}
			rs.close();
			st.close();
			
			if (!KeyFound)
			{
				// key does not exist - create it
				System.out.println("Key not found, inserting ("+ObjectType+",1");
				PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_keys(object,id) VALUES (?,?)");
				st2.setString(1, ObjectType);
				st2.setInt(2, newval);
				st2.execute();
				st2.close();
			}
			rs.close();
			getDBConnection().commit();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return res;
	}
	
 public int getSchemaVersion()
 {
  int res = 0;
  try
  {
   PreparedStatement st = getDBConnection().prepareStatement("SELECT schemaver FROM dm.dm_tableinfo");
   ResultSet rs = st.executeQuery();
   if(rs.next()) {
    res = rs.getInt(1);
    rs.close();
    return res;
   } 
  }
  catch (SQLException e)
  {
   e.printStackTrace();
   rollback();
  }
  return res;
 }
 
 public String getLicenseKey()
 {
	 String res = "";
	 try {
		 PreparedStatement st = getDBConnection().prepareStatement("SELECT status FROM dm.dm_tableinfo");
		 ResultSet rs = st.executeQuery();
		 if(rs.next()) {
			 res = rs.getString(1);
			 rs.close();
			 return res;
		 } 
	 } catch (SQLException e) {
		 e.printStackTrace();
		 rollback();
	 }
	 return res;
 }
	
	public int GetDomainForObject(String objtype,int id)
	{
	 if (objtype.equalsIgnoreCase("release"))
	  objtype = "application";
	 else if (objtype.equalsIgnoreCase("builder"))
	  objtype = "buildengine";
	 
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("SELECT domainid FROM dm.dm_"+objtype+" WHERE id=?");
			st.setInt(1, id);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				return rs.getInt(1);
			}
			else
			{
				return 0;
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return 0;
	}


	public ObjectTypeAndId CreateNewObject(String objtype,String objname,int domainid,int parentid,int id,int xp,int yp, String treeid, boolean commit)
	{
		System.out.println("CreateNewObject(objtype="+objtype+" objname="+objname+" domainid="+domainid+" parentid="+parentid+" id="+id + " treeid=" + treeid);
		ObjectTypeAndId ret = null;

		if (objname.replaceAll("[A-Za-z0-9_ ]","").length()>0) {
			throw new RuntimeException("Invalid Object Name");
		}
		
		try
		{
			PreparedStatement st;
			PreparedStatement cs = null;
			long t = timeNow();
			if (objtype.equalsIgnoreCase("user"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_user WHERE name=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_user(id,name,domainid,creatorid,modifierid,created,modified,locked,status) VALUES(?,?,?,?,?,?,?,'N','N')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,domainid);
				st.setInt(4, getUserID());
				st.setInt(5,getUserID());
				st.setLong(6,t);
				st.setLong(7,t);
				cs.setString(1,objname);
				ret = new ObjectTypeAndId(ObjectType.USER, id);
			}
			else
			if (objtype.equalsIgnoreCase("release"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_application WHERE name=? AND domainid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_application(id,name,domainid,ownerid,creatorid,modifierid,created,modified,status,isRelease) VALUES(?,?,?,?,?,?,?,?,'N','Y')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,domainid);
				st.setInt(4, getUserID());
				st.setInt(5,getUserID());
				st.setInt(6,getUserID());
				st.setLong(7,t);
				st.setLong(8,t);
				cs.setString(1,objname);
				cs.setInt(2,domainid);
				ret = new ObjectTypeAndId(ObjectType.RELEASE, id);
			}
			else
			if (objtype.equalsIgnoreCase("application"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_application WHERE name=? AND domainid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_application(id,name,domainid,ownerid,creatorid,modifierid,created,modified,status,isRelease) VALUES(?,?,?,?,?,?,?,?,'N','N')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,domainid);
				st.setInt(4, getUserID());
				st.setInt(5,getUserID());
				st.setInt(6,getUserID());
				st.setLong(7,t);
				st.setLong(8,t);
				cs.setString(1,objname);
				cs.setInt(2,domainid);
				ret = new ObjectTypeAndId(ObjectType.APPLICATION, id);
			}
			else			 
			if (objtype.equalsIgnoreCase("appversion"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_application WHERE name=? AND domainid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_application(id,name,domainid,ownerid,creatorid,modifierid,created,modified,parentid,predecessorid,status,isRelease) VALUES(?,?,?,?,?,?,?,?,?,?,'N','N')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,domainid);
				st.setInt(4, getUserID());
				st.setInt(5,getUserID());
				st.setInt(6,getUserID());
				st.setLong(7,t);
				st.setLong(8,t);
				st.setInt(9,parentid);
				st.setInt(10,parentid);
				cs.setString(1,objname);
				cs.setInt(2,domainid);
				ret = new ObjectTypeAndId(ObjectType.APPVERSION, id);
			}
			else
			if (objtype.equalsIgnoreCase("relversion"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_application WHERE name=? AND domainid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_application(id,name,domainid,ownerid,creatorid,modifierid,created,modified,parentid,predecessorid,status,isRelease) VALUES(?,?,?,?,?,?,?,?,?,?,'N','Y')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,domainid);
				st.setInt(4, getUserID());
				st.setInt(5,getUserID());
				st.setInt(6,getUserID());
				st.setLong(7,t);
				st.setLong(8,t);
				st.setInt(9,parentid);
				st.setInt(10,parentid);
				cs.setString(1,objname);
				cs.setInt(2,domainid);
				ret = new ObjectTypeAndId(ObjectType.RELVERSION, id);
			}
			else			 
			if (objtype.equalsIgnoreCase("componentitem"))
			{
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_componentitem(id,name,creatorid,modifierid,created,modified,compid,status) VALUES(?,?,?,?,?,?,?,'N')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,getUserID());
				st.setInt(4,getUserID());
				st.setLong(5,t);
				st.setLong(6,t);
				st.setInt(7,parentid);
				ret = new ObjectTypeAndId(ObjectType.COMPONENTITEM, id);
			}
			else if(objtype.equalsIgnoreCase("credentials"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_credentials WHERE name=? AND domainid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_credentials(id,name,domainid,kind,creatorid,modifierid,created,modified,status) VALUES(?,?,?,?,?,?,?,?,'N')");	// ,status ,'N'
				st.setInt(1, id);
				st.setString(2, objname);
				st.setInt(3, domainid);
				st.setInt(4, 0);	// unconfigured
				st.setInt(5,getUserID());
				st.setInt(6,getUserID());
				st.setLong(7,t);
				st.setLong(8,t);
				cs.setString(1,objname);
				cs.setInt(2,domainid);
				ret = new ObjectTypeAndId(ObjectType.CREDENTIALS, id);
			}
			else if(objtype.equalsIgnoreCase("repository") || objtype.equalsIgnoreCase("notify")
					|| objtype.equalsIgnoreCase("datasource") || objtype.equalsIgnoreCase("buildengine"))
			{
				// TODO: Status should be 'U' for unconfigured - can only do this when we have a mechanism to test it and change status to Normal
				cs = getDBConnection().prepareStatement("SELECT count(*) FROM dm.dm_"+objtype+" WHERE name=? AND domainid=?");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_"+objtype+"(id,name,domainid,ownerid,defid,creatorid,modifierid,created,modified,status) VALUES(?,?,?,?,?,?,?,?,?,'N')");
				st.setInt(1, id);
				st.setString(2, objname);
				st.setInt(3, domainid);
				st.setInt(4, getUserID());
				st.setInt(5, 0);	// unconfigured
				st.setInt(6, getUserID());
				st.setInt(7, getUserID());
				st.setLong(8, t);
				st.setLong(9, t);
				cs.setString(1,objname);
				cs.setInt(2,domainid);
				ObjectType ot = ObjectType.fromTableName(objtype);
				if (ot == null) {
					throw new RuntimeException("Unable to create object of type '" + objtype + "'");
				}
				ret = new ObjectTypeAndId(ot, id);
			}
			else if (objtype.equalsIgnoreCase("template"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_template WHERE name=? AND notifierid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_template(id,name,notifierid,creatorid,modifierid,created,modified,status) VALUES(?,?,?,?,?,?,?,'N')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,parentid);
				st.setInt(4,getUserID());
				st.setInt(5,getUserID());
				st.setLong(6,t);
				st.setLong(7,t);
				cs.setString(1,objname);
				cs.setInt(2,parentid);
				ObjectType ot = ObjectType.fromTableName(objtype);
				if (ot == null) {
					throw new RuntimeException("Unable to create object of type '" + objtype + "'");
				}
				ret = new ObjectTypeAndId(ot, id);
			}
			else if (objtype.equalsIgnoreCase("component"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_component WHERE name=? AND domainid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_component(id,name,domainid,ownerid,creatorid,modifierid,created,modified,status,filteritems,deployalways) VALUES(?,?,?,?,?,?,?,?,'N','Y','Y')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,domainid);
				st.setInt(4, getUserID());
				st.setInt(5,getUserID());
				st.setInt(6,getUserID());
				st.setLong(7,t);
				st.setLong(8,t);
				cs.setString(1,objname);
				cs.setInt(2,domainid);
				ret = new ObjectTypeAndId(ObjectType.COMPONENT, id);
			}
			else if (objtype.equalsIgnoreCase("buildjob"))
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_buildjob WHERE name=? AND builderid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_buildjob(id,name,builderid,creatorid,modifierid,created,modified,status) VALUES(?,?,?,?,?,?,?,'N')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,parentid);
				st.setInt(4,getUserID());
				st.setInt(5,getUserID());
				st.setLong(6,t);
				st.setLong(7,t);
				cs.setString(1,objname);
				cs.setInt(2,parentid);
				ObjectType ot = ObjectType.fromTableName(objtype);
				if (ot == null) {
					throw new RuntimeException("Unable to create object of type '" + objtype + "'");
				}
				ret = new ObjectTypeAndId(ot, id);
			}
			else
			{
				cs = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_"+objtype+" WHERE name=? AND domainid=? AND status='N'");
				st = getDBConnection().prepareStatement("INSERT INTO dm.dm_"+objtype+"(id,name,domainid,ownerid,creatorid,modifierid,created,modified,status) VALUES(?,?,?,?,?,?,?,?,'N')");
				st.setInt(1, id);
				st.setString(2,objname);
				st.setInt(3,domainid);
				st.setInt(4, getUserID());
				st.setInt(5,getUserID());
				st.setInt(6,getUserID());
				st.setLong(7,t);
				st.setLong(8,t);
				cs.setString(1,objname);
				cs.setInt(2,domainid);
				ObjectType ot = ObjectType.fromTableName(objtype);
				if (ot == null) {
					throw new RuntimeException("Unable to create object of type '" + objtype + "'");
				}
				ret = new ObjectTypeAndId(ot, id);
			}
			boolean okToCreate = false;
			if (cs != null) {
				ResultSet rs = cs.executeQuery();
				if (rs.next()) {
					int c = rs.getInt(1);
					if (c==0) {
						okToCreate = true;
					} else {
						rs.close();
						cs.close();
						st.close();
						String ots = ret.getObjectType().toString();
						if (objtype.equalsIgnoreCase("user")) {
							throw new RuntimeException("User "+objname+" already exists");
						} else {
							throw new RuntimeException(ots.substring(0,1)+ots.substring(1).toLowerCase()+" "+objname+" already exists in this domain");
						}
					}
				} else {
					// count has failed for some reason
					rs.close();
					cs.close();
					st.close();
					throw new RuntimeException("Could not get count of existing objects");
				}
			} else {
				okToCreate = true;
			}
			if (okToCreate) {
				// An object with this name does not exist in this domain
				st.execute();	// execute the insert
				int rowCount = st.getUpdateCount();
				if (rowCount != 1) {
					if (cs != null) {
						cs.close();
					}
					st.close();
					throw new RuntimeException("Unable to create object of type '" + objtype + "'");
				}
			}
			if (cs != null) {
				cs.close();
			}
			st.close();
			
			if (objtype.equalsIgnoreCase("server") && !treeid.contains("servers"))
			{
				// creating a server inside an environment
				int ServerWidth=120;
				int ServerHeight=70;
				ArrayList<Integer> cx=new ArrayList<Integer>();
				ArrayList<Integer> cy=new ArrayList<Integer>();
				PreparedStatement st2 = getDBConnection().prepareStatement("SELECT xpos,ypos FROM dm.dm_serversinenv WHERE envid=?");
				st2.setInt(1,parentid);
				ResultSet rs2 = st2.executeQuery();
				while (rs2.next()) {
					System.out.println("ArrayList xpos="+rs2.getInt(1)+" ypos="+rs2.getInt(2));
					cx.add(rs2.getInt(1));
					cy.add(rs2.getInt(2));
				}
				rs2.close();
				st2.close();
				int tgtx=0;
				int tgty=0;
				if (xp == -1 || yp == -1) {
					boolean found=false;
					for (int ypos=0;ypos<=1000 && !found;ypos=ypos+100) {
						for (int xpos=0;xpos<=1000 && !found;xpos=xpos+100) {
							//
							// Check if anything is located at the current xpos/ypos position and, if not, place
							// the new server there.
							// 
							found=true;
							for (int i=0;i<cx.size();i++) {
								found=false;
								Integer x = cx.get(i);
								Integer y = cy.get(i);
								System.out.println("Checking xpos="+xpos+" ypos="+ypos+" against x="+x+" y="+y);
								if (xpos >= x && xpos <= x+ServerWidth) break;
								System.out.println("NOT xpos >=x and <= x+"+ServerWidth);
								if (ypos >= y && ypos <= y+ServerHeight) break;
								System.out.println("NOT ypos >=y and <= y+"+ServerHeight);
								tgtx=xpos;
								tgty=ypos;
								found=true;
							}
							System.out.println("found="+found);
						}
					}
				} else {
					tgtx=xp;
					tgty=yp;
				}
				PreparedStatement st3 = getDBConnection().prepareStatement("INSERT INTO dm.dm_serversinenv(envid,serverid,xpos,ypos) values(?,?,?,?);");
				st3.setInt(1,parentid);
				st3.setInt(2,id);
				st3.setInt(3,tgtx);
				st3.setInt(4,tgty);
				st3.execute();
				st3.close();
				PreparedStatement st4 = getDBConnection().prepareStatement("UPDATE dm.dm_environment SET modified=?,modifierid=?");
				st4.setLong(1,t);
				st4.setInt(2, getUserID());
				st4.execute();
				st4.close();
			}
			
			if (objtype.equalsIgnoreCase("domain"))
			{
				// need to add this domain to our allowed domain list.
				m_domains.put(id,"Y");
				m_domainlist=m_domainlist+","+id;
				
			}
			if (commit) {
				getDBConnection().commit();
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return ret;
	}
	
	public void CreateNewObject(String objtype,String objname,int domainid,int parentid,int id, String treeid)
	{
		CreateNewObject(objtype,objname,domainid,parentid,id,-1,-1,treeid,true);
	}
	
	public ObjectTypeAndId CreateNewObject(String objtype,String objname,int domainid,int parentid,int id,int xp,int yp, String treeid)
	{
		return CreateNewObject(objtype,objname,domainid,parentid,id,xp,yp,treeid,true);
	}
	
	public void addToCategory(int catid,ObjectTypeAndId otid,boolean commit)
	{
		// Adds the specified object to the specified category
		String cattab=null;
		switch(otid.getObjectType()) {
		case PROCEDURE:
		case FUNCTION:
		case ACTION:
			cattab="dm_action_categories";
			break;
		case COMPONENT:
			cattab="dm_component_categories";
			break;
		case FRAGMENT:
			cattab="dm_fragment_categories";
			break;
		default:
			break;
		} 
		String csql = "SELECT count(*) FROM "+cattab+" WHERE id=? AND categoryid=?";
		String isql = "INSERT INTO "+cattab+"(id,categoryid) VALUES(?,?)";
		try {
			PreparedStatement cstmt = getDBConnection().prepareStatement(csql);
			cstmt.setInt(1,otid.getId());
			cstmt.setInt(2,catid);
			ResultSet rs = cstmt.executeQuery();
			if (rs.next()) {
				if (rs.getInt(1)==0) {
					// No existing row - insert it
					PreparedStatement istmt = getDBConnection().prepareStatement(isql);
					istmt.setInt(1,otid.getId());
					istmt.setInt(2,catid);
					istmt.execute();
					istmt.close();
				}
			}
			rs.close();
			cstmt.close();
			if (commit) {
				getDBConnection().commit();
			}
		} catch(SQLException ex) {
			ex.printStackTrace();
			rollback();
		}
	}
	
	public void addToCategory(int catid,ObjectTypeAndId otid)
	{
		addToCategory(catid,otid,false);
	}
	
	public ObjectTypeAndId CreateNewAction(String actiontype,String objname,int domainid,int parentid,int id)
	{
		System.out.println("CreateNewAction(actiontype="+actiontype+" objname="+objname+" domainid="+domainid+" parentid="+parentid+" id="+id);
		ObjectTypeAndId ret = null;
		try
		{
			String csql = "SELECT count(*) FROM dm.dm_action WHERE name=? AND domainid=?";
			String sql1="INSERT INTO dm.dm_action(id,name,domainid,ownerid,\"function\",graphical,status,"
					+ "creatorid,created,modifierid,modified,kind) VALUES(?,?,?,?,?,?,'N',?,?,?,?,?)";
			String sql2="INSERT INTO dm.dm_action(id,name,domainid,ownerid,\"function\",graphical,status,"
					+ "creatorid,created,modifierid,modified,kind,textid) VALUES(?,?,?,?,?,?,'N',?,?,?,?,?,?)";
			ActionKind kind = ActionKind.UNCONFIGURED;
			ObjectType ot = ObjectType.PROCEDURE;
			String g="N";
			String f="N";
			if (actiontype.equalsIgnoreCase("g")) {
				kind = ActionKind.GRAPHICAL;
				ot = ObjectType.ACTION;
				g="Y";
			} else if(actiontype.equalsIgnoreCase("f")) {
				ot = ObjectType.FUNCTION;
				f="Y";
			}
			// Check the Action/Function/Procedure doesn't already exist in this domain
			PreparedStatement cs = getDBConnection().prepareStatement(csql);
			cs.setString(1,objname);
			cs.setInt(2,domainid);
			ResultSet rs = cs.executeQuery();
			if (rs.next()) {
				int c = rs.getInt(1);
				if (c>0) {
					rs.close();
					cs.close();
					throw new RuntimeException("A "+ot.toString().substring(0,1)+ot.toString().substring(1).toLowerCase()+" called \""+objname+"\" already exists in this domain");
				}
			} else {
				throw new RuntimeException("Failed to get count of existing actions");
			}
			long t = timeNow();
			System.out.println("ActionKind = "+kind.value());
			int atval=0;
			if (kind == ActionKind.IN_DB) {
				// Create an entry in dm_actiontext (stored procedure/function)
				atval = getID("actiontext");
				PreparedStatement at = getDBConnection().prepareStatement("INSERT INTO dm.dm_actiontext(id) VALUES(?)");
				at.setInt(1,atval);
				at.execute();
				
			}
			PreparedStatement st = getDBConnection().prepareStatement((kind == ActionKind.IN_DB)?sql2:sql1);
			st.setInt(1, id);
			st.setString(2,objname);
			st.setInt(3,domainid);
			st.setInt(4, getUserID());
			st.setString(5, f);
			st.setString(6, g);
			st.setInt(7, getUserID());
			st.setLong(8, t);
			st.setInt(9, getUserID());
			st.setLong(10, t);
			st.setInt(11, kind.value());
			if (kind == ActionKind.IN_DB) {
				st.setInt(12,atval);
			}
			st.execute();
			st.close();
			
			getDBConnection().commit();
			ret = new ObjectTypeAndId(ot, id);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return ret;
	}
	
	private int GetTaskType(String tasktype)
	{
		try
		{
			System.out.println("tasktype="+tasktype);
			PreparedStatement st = getDBConnection().prepareStatement("SELECT id FROM dm.dm_tasktypes WHERE name=?");
			st.setString(1,tasktype);
			ResultSet rs = st.executeQuery();
			if(rs.next()) {
				int res = rs.getInt(1);
				rs.close();
				return res;
			}
			throw new RuntimeException("Unable to retrieve task type '" + tasktype + "' from database");
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return 0;
	}
	
	public void CreateNewTask(String taskname,String tasktype,int domainid,int id)
	{
		int typeid = GetTaskType(tasktype);
		try
		{
			long t = timeNow();
			PreparedStatement st = getDBConnection().prepareStatement("INSERT INTO dm.dm_task(id,name,typeid,domainid,ownerid,creatorid,created,modifierid,modified,logoutput,subdomains) VALUES(?,?,?,?,?,?,?,?,?,?,?)");
			st.setInt(1, id);
			st.setString(2,taskname);
			st.setInt(3, typeid);
			st.setInt(4,domainid);
			st.setInt(5, getUserID());
			st.setInt(6,getUserID());
			st.setLong(7,t);
			st.setInt(8,getUserID());
			st.setLong(9,t);
			st.setString(10,"N");
			st.setString(11,"N");
			st.execute();
			st.close();
			getDBConnection().commit();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
	}
	
	public void DeleteTask(int taskid,int domainid)
	{
		try
		{
			Task task = getTask(taskid,true);
			System.out.println("tasktype="+task.getTaskType());
			PreparedStatement st = getDBConnection().prepareStatement("DELETE FROM dm.dm_taskaccess WHERE taskid=?");
			st.setInt(1, taskid);
			st.execute();
			st.close();
			String table = null;
			switch(task.getTaskType()) {
			case APPROVE: 		 table = "approve"; break;
			case CREATE_VERSION: table = "createversion"; break;
			case MOVE: 			 table = "move"; break;
			case REQUEST: 		 table = "request"; break;
			default: break;
			}
			if(table != null) {
				PreparedStatement st2 = getDBConnection().prepareStatement("DELETE FROM dm.dm_task" + table + " WHERE id=?");
				st2.setInt(1, taskid);
				st2.execute();
				st2.close();
			}
			PreparedStatement st3 = getDBConnection().prepareStatement("DELETE FROM dm.dm_request WHERE taskid=?");
			st3.setInt(1, taskid);
			st3.execute();
			st3.close();
			PreparedStatement st4 = getDBConnection().prepareStatement("DELETE FROM dm.dm_taskparams WHERE taskid=?");
			st4.setInt(1, taskid);
			st4.execute();
			st4.close();
			PreparedStatement st5 = getDBConnection().prepareStatement("DELETE FROM dm.dm_task WHERE id=? and domainid=?");
			st5.setInt(1, taskid);
			st5.setInt(2,domainid);
			st5.execute();
			st5.close();
			// Update any linked task so it can be modified
			PreparedStatement st6 = getDBConnection().prepareStatement("UPDATE dm.dm_taskrequest SET linkedtaskid = NULL WHERE linkedtaskid=?");
			st6.setInt(1, taskid);
			st6.execute();
			st6.close();
			getDBConnection().commit();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
	}
		
	
	public TaskList getTasksInDomain(int domainid)
	{
		TaskList res = new TaskList();
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("SELECT a.id,a.name,b.name FROM dm.dm_task a,dm.dm_tasktypes b where b.id=a.typeid and a.domainid=?");
			st.setInt(1,domainid);
			ResultSet rs = st.executeQuery();
			while (rs.next())
			{
				Task t = new Task();
				t.setId(rs.getInt(1));
				t.setName(rs.getString(2));
				t.setTaskType(rs.getString(3));
				res.add(t);
			}
			rs.close();
			st.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return res;
	}
	
	public int addTaskParameter(int tid,String label,String varname,String vartype,String arrname)
	{
		try {
			int pos=1;
			PreparedStatement ct = getDBConnection().prepareStatement("SELECT max(pos) FROM dm.dm_taskparams WHERE taskid=?");
			ct.setInt(1,tid);
			ResultSet rs = ct.executeQuery();
			if (rs.next()) {
				pos = rs.getInt(1)+1;
				if (rs.wasNull()) pos=1;
			}
			rs.close();
			ct.close();
			PreparedStatement st = getDBConnection().prepareStatement("INSERT INTO dm.dm_taskparams(taskid,pos,label,variable,type,arrname) VALUES(?,?,?,?,?,?)");
			st.setInt(1,tid);
			st.setInt(2,pos);
			st.setString(3,label);
			st.setString(4,varname);
			st.setString(5,vartype);
			if (arrname != null) {
				st.setString(6,arrname);
			} else {
				st.setNull(6,Type.CHAR);
			}
			st.execute();
			st.close();
			getDBConnection().commit();
			return pos;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 1;	// default (fail) condition
	}
	
	public void editTaskParameter(int tid,int pos,String label,String varname,String vartype,String arrname)
	{
		try {
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_taskparams set label=?,variable=?,type=?,arrname=? WHERE taskid=? AND pos=?");
			st.setString(1,label);
			st.setString(2,varname);
			st.setString(3,vartype);
			if (arrname != null) {
				st.setString(4,arrname);
			} else {
				st.setNull(4,Type.CHAR);
			}
			st.setInt(5,tid);
			st.setInt(6,pos);
			st.execute();
			st.close();
			getDBConnection().commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteTaskParameter(int tid,String varname)
	{
		try {
			PreparedStatement st1 = getDBConnection().prepareStatement("SELECT pos FROM dm.dm_taskparams WHERE taskid=? AND variable=?");
			st1.setInt(1,tid);
			st1.setString(2,varname);
			ResultSet rs1 = st1.executeQuery();
			if (rs1.next()) {
				int pos = rs1.getInt(1);
				PreparedStatement st2 = getDBConnection().prepareStatement("UPDATE dm.dm_taskparams SET pos=pos-1 WHERE taskid=? AND pos>?");
				st2.setInt(1,tid);
				st2.setInt(2,pos);
				st2.execute();
				st2.close();
				PreparedStatement st3 = getDBConnection().prepareStatement("DELETE FROM dm.dm_taskparams WHERE taskid=? AND variable=?");
				st3.setInt(1,tid);
				st3.setString(2,varname);
				st3.execute();
				st3.close();
			}
			rs1.close();
			st1.close();
			getDBConnection().commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void changeTaskParameterPos(int tid,String varname,int newpos)
	{
		try {
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_taskparams set pos=? WHERE taskid=? AND variable=?");
			st.setInt(1,newpos);
			st.setInt(2,tid);
			st.setString(3,varname);
			System.out.println("Changing variable "+varname+" to position "+newpos+" for task id "+tid);
			st.execute();
			st.close();
			getDBConnection().commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<TaskParameter> getTaskParameters(int tid)
	{
		List <TaskParameter> ret = new ArrayList<TaskParameter>();
		try {
			PreparedStatement st = getDBConnection().prepareStatement("SELECT pos,label,variable,type,arrname FROM dm.dm_taskparams WHERE taskid=? ORDER BY pos");
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				if (rs.getString(4).trim().equalsIgnoreCase("dropdown")) {
					ret.add(new TaskParameter(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)));
				} else {
					ret.add(new TaskParameter(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4)));
				}
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public List<DMAttribute> getArrayValuesForObject(ObjectType ot,int objid,String arrname)
	{
		// Returns any array values specified against the given object
		try {
			List<DMAttribute> ret = new ArrayList<DMAttribute>();
			String tn=null;
			String cn=null;
			switch(ot) {
			case ENVIRONMENT:
				tn="dm.dm_environmentvars";
				cn="envid";
				break;
			case APPLICATION:
				tn="dm.dm_applicationvars";
				cn="appid";
				break;
			default:
				break;
			}
			PreparedStatement st = getDBConnection().prepareStatement("select b.name,b.value from "+tn+" a,dm_arrayvalues b where a."+cn+" = ? and a.arrayid=b.id and a.name=? order by b.id");
			st.setInt(1,objid);
			st.setString(2,arrname);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				ret.add(new DMAttribute(rs.getString(1), rs.getString(2)));
			}
			rs.close();
			st.close();
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
 
 public TaskList getAccessibleTasks()
 {
  TaskList res = new TaskList();
  try
  {
   PreparedStatement st = getDBConnection().prepareStatement("SELECT a.id,a.name,b.name FROM dm.dm_task a,dm.dm_tasktypes b where b.id=a.typeid and a.domainid in (" + m_domainlist + ") order by 2");
   ResultSet rs = st.executeQuery();
   while (rs.next())
   {
    Task t = new Task();
    t.setId(rs.getInt(1));
    t.setName(rs.getString(2));
    t.setTaskType(rs.getString(3));
    res.add(t);
   }
   rs.close();
   st.close();
  }
  catch (SQLException e)
  {
   e.printStackTrace();
   rollback();
  }
  return res;
 }
 
 	private void GetWorkbenchTasksInternal(int domainid,String objecttype, PropertyDataSet ds, boolean inherit)
 	{
 		System.out.println("GetWorkbenchTasksInternal - objectype="+objecttype+" domainid="+domainid+" m_userID="+getUserID());
		 
		if (objecttype.equalsIgnoreCase("release"))
		objecttype = "application";

		try
		{
			
			String sql = 	"SELECT DISTINCT a.id,a.name,b.name			"
			+	"FROM	dm.dm_task			a,	"
			+	"		dm.dm_tasktypes		b,	"
			+	"		dm.dm_taskaccess	c,	"
			+ 	"		dm.dm_usersingroup	d	"
			+	"WHERE	b."+objecttype+"='Y'	"
			+	"AND	a.typeid=b.id			"
			+	"AND	a.domainid=?			"
			+	"AND	c.taskid=a.id			"
			+	"AND	((c.usrgrpid=d.groupid	"
			+	"AND	d.userid=?)				"
			+   "OR		(c.usrgrpid="+UserGroup.EVERYONE_ID+"))";
			
			if (inherit) sql+= " AND a.subdomains='Y'";
			System.out.println(sql);
			PreparedStatement st = getDBConnection().prepareStatement(sql);
			st.setInt(1,domainid);
			st.setInt(2,getUserID());
			ResultSet rs = st.executeQuery();
			while (rs.next())
			{
				System.out.println("rs.getString(1)="+rs.getString(1));
				String tasktype = rs.getString(3);
				ds.addProperty(rs.getString(2), tasktype+"-"+rs.getString(1));
			}
			rs.close();
			st.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		// Now move up the domain heirarchy looking for any inherited tasks
		Domain domain = getDomain(domainid);
		if (domain != null) {
			Domain parentDomain = domain.getDomain();
			if (parentDomain != null) {
				System.out.println("Looking in parent domain "+parentDomain.getId());
				GetWorkbenchTasksInternal(parentDomain.getId(),objecttype,ds,true);
			}
		}
 	}
 	
	public void GetWorkbenchTasks(int domainid,String objecttype, PropertyDataSet ds)
	{
		System.out.println("GetWorkbenchTasks");
		GetWorkbenchTasksInternal(domainid,objecttype,ds,false);
		System.out.println("GetWorkbenchTasks returns");
	}

	
	public String GetUserName(int uid)
	{
		String UserName = "";
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT name,domainid FROM dm.dm_user where id="+uid);
			while (rs.next())
			{
				if (ValidDomain(rs.getInt(2),true))
				{
					UserName = rs.getString(1);
				}
			}
			rs.close();
			st.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return UserName;
	}
	
	public String GetUserName()
	{
		return GetUserName(getUserID());
	}
	
	public String getDomainName(int domainid)
	{
		String res = "";
		if (ValidDomain(domainid))
		{
			try
			{
				PreparedStatement st = getDBConnection().prepareStatement("select name,domainid from dm.dm_domain where id=?");
				st.setInt(1, domainid);
				ResultSet rs = st.executeQuery();
				if (rs.next())
				{
					res = rs.getString(1);
					int parentDomainId = getInteger(rs, 2, 0);
					if(parentDomainId != 0) {
						String ParentDomains = getDomainName(parentDomainId);
						if (ParentDomains != "")
						{
							res = ParentDomains + "." + res;
						}
					}
				}
				rs.close();
			}
			catch (Exception e)
		    {
				e.printStackTrace();
		    }
		}
		return res;
	}
	
	public boolean userIsReferenced(int id)
	{
		return true;
	}
	
	public boolean userGroupIsReferenced(int id)
	{
		return true;
	}
	
	private boolean CheckObjects(String sql[],int id)
	{
		try
		{
			boolean res=false;
			for (int i=0;i<sql.length;i++) {
				PreparedStatement stmt = getDBConnection().prepareStatement(sql[i]);
				stmt.setInt(1,id);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					System.out.println(sql[i]+" = "+ rs.getInt(1));
					if (rs.getInt(1)>0) {
						res=true;
						break;
					}
					rs.close();
				} else {
					res=true;	// failsafe (if we get here, no rows were retrieved)
				}
				stmt.close();
			}
			return res;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve object count for object id " + id);
	}
	
	public boolean envIsReferenced(int envid)
	{
		String sql[] = {
				"SELECT count(*) FROM dm.dm_deployment WHERE envid=?"
		 };
		 return CheckObjects(sql,envid);
	}
	
	public boolean componentIsReferenced(int compid,boolean forMenu, boolean isRelease) {
		System.out.println("in componentIsReferenced("+compid+","+(forMenu?"true":"false")+")");
		// Returns true if component is associated with an application or server, false otherwise
		
		if (forMenu) {
			if (isRelease) {
				String sql[] = {
					"SELECT count(*) FROM dm.dm_applicationcomponent WHERE childappid=?",
					"SELECT count(*) FROM dm.dm_compsallowedonserv WHERE compid=?"
				};
				return CheckObjects(sql,compid);
			} else {
				if (isRelease) {
					String sql[] = {
						"SELECT count(*) FROM dm.dm_applicationcomponent WHERE childappid=?",
						"SELECT count(*) FROM dm.dm_compsallowedonserv WHERE compid=?"
					};
					return CheckObjects(sql,compid);
				} else {
					String sql[] = {
					"SELECT count(*) FROM dm.dm_applicationcomponent WHERE compid=?",
					"SELECT count(*) FROM dm.dm_compsallowedonserv WHERE compid=?"
					};
					return CheckObjects(sql,compid);
				}
			}
		} else {
			if (isRelease) {
				String sql[] = {
					"SELECT count(*) FROM dm.dm_applicationcomponent WHERE childappid=?",
					"SELECT count(*) FROM dm.dm_compsallowedonserv WHERE compid=?",
					"SELECT count(*) FROM dm.dm_compsonserv WHERE compid=?",
					"SELECT count(*) FROM dm.dm_deploymentxfer WHERE componentid=?"
				};
				return CheckObjects(sql,compid);
			} else {
				String sql[] = {
				"SELECT count(*) FROM dm.dm_applicationcomponent WHERE compid=?",
				"SELECT count(*) FROM dm.dm_compsallowedonserv WHERE compid=?",
				"SELECT count(*) FROM dm.dm_compsonserv WHERE compid=?",
				"SELECT count(*) FROM dm.dm_deploymentxfer WHERE componentid=?"
				};
				return CheckObjects(sql,compid);
			}		 
		}
 }
	
 public boolean serverIsReferenced(int servid) {
  
    String sql[] = {
   "SELECT count(*) FROM dm.dm_serversinenv a, dm.dm_server b WHERE serverid=? and b.status <> 'D' and a.serverid = b.id"
    };
    return CheckObjects(sql,servid);
 }

 public boolean procfuncIsReferenced(int id) {
  
  String sql[] = {
 "SELECT count(*) FROM dm.dm_actionfrags a, dm.dm_fragments b WHERE b.id=a.typeid and ? in (b.actionid,b.functionid)"
  };
  return CheckObjects(sql,id);
}
 
 public boolean actionIsReferenced(int id) {
  String sql[] = {
 "SELECT count(*) FROM dm.dm_deploymentaction where actionid=?"
  };
  return CheckObjects(sql,id);
 }
 
 public boolean notifierIsReferenced(int id) {
  String sql[] = {
  "SELECT count(*) FROM dm.dm_template x WHERE x.notifierid=? AND x.id IN "
  +"(SELECT successtemplateid FROM dm.dm_application WHERE successtemplateid=x.id "
  +"UNION "
  +"SELECT failuretemplateid FROM dm.dm_application WHERE failuretemplateid=x.id "
  +"UNION "
  +"SELECT successtemplateid FROM dm.dm_task WHERE successtemplateid=x.id "
  +"UNION "
  +"SELECT failuretemplateid FROM dm.dm_task WHERE failuretemplateid=x.id "
  + ")"
  };
  return CheckObjects(sql,id);
 }
 
 public boolean templateIsReferenced(int id) {
	  String sql[] = {
	  "SELECT count(*) FROM dm.dm_application where successtemplateid=?",
	  "SELECT count(*) FROM dm.dm_application where failuretemplateid=?",
	  "SELECT count(*) FROM dm.dm_task where successtemplateid=?",
	  "SELECT count(*) FROM dm.dm_task where failuretemplateid=?"
	  };      
	  return CheckObjects(sql,id);
 }  
 
 public boolean datasourceIsReferenced(int id) {
     String sql[] = {
      "SELECT count(*) FROM dm.dm_user where datasourceid=?"
     };
     return CheckObjects(sql,id);
 }
 
 public boolean comptypeIsReferenced(int id) {
  
  String sql[] = {
 "SELECT count(*) FROM dm.dm_servercomptype a WHERE  a.comptypeid =?",
 "SELECT count(*) FROM dm.dm_component a WHERE  a.comptypeid =?"
  };
  return CheckObjects(sql,id);
}
 
 public boolean repositoryIsReferenced(int id) {
  
  String sql[] = {
   "SELECT count(*) FROM dm.dm_componentitem a WHERE repositoryid=? and a.status <> 'D'",
   "SELECT count(*) FROM dm.dm_action a WHERE repositoryid=? and a.status <> 'D'",
   "SELECT count(*) FROM dm.dm_deploymentxfer WHERE repoid=?"
  };
  return CheckObjects(sql,id);
}

 public boolean credentialIsReferenced(int id) {
  
  String sql[] = {
   "SELECT count(*) FROM dm.dm_server a WHERE credid=? and a.status <> 'D'",
   "SELECT count(*) FROM dm.dm_datasource d WHERE credid=? AND d.status <> 'D'",
   "SELECT count(*) FROM dm.dm_repository r WHERE credid=? AND r.status <> 'D'",
   "SELECT count(*) FROM dm.dm_notify WHERE credid=? AND status <> 'D'",
   "SELECT count(*) FROM dm.dm_environment e WHERE credid=? and e.status <> 'D'",
   "SELECT count(*) FROM dm.dm_buildengine e WHERE credid=? and e.status <> 'D'"
  };
  return CheckObjects(sql,id);
}
 
 public boolean applicationIsReferenced(int id) {
  
  String sql[] = {
   "SELECT count(*) FROM dm.dm_applicationcomponent a WHERE appid=?",
   "SELECT count(*) FROM dm.dm_appsallowedinenv a WHERE appid=?", 
   "SELECT count(*) FROM dm.dm_appsinenv a WHERE appid=?",
   "SELECT count(*) FROM dm.dm_deployment a WHERE appid=?"
  };
  return CheckObjects(sql,id);
}

 
	public int DeleteFromTable(String TableName,String ColName,int id) throws SQLException
	{
		PreparedStatement stmt = getDBConnection().prepareStatement("DELETE FROM "+TableName+" where "+ColName+"=?");
		System.out.println("DELETE FROM "+TableName+" where "+ColName+"=?" + id);
		
		stmt.setInt(1,id);
		stmt.execute();
		int res = (stmt.getUpdateCount()<1)?1:0;
		stmt.close();
		return res;
	}
	
	public int DeleteFromTableWhere(String TableName,String WhereClause,int id) throws SQLException
	{
		PreparedStatement stmt = getDBConnection().prepareStatement("DELETE FROM "+TableName+" WHERE "+WhereClause);
		stmt.setInt(1,id);
		stmt.execute();
		int res = (stmt.getUpdateCount()<1)?1:0;
		stmt.close();
		return res;
	}
	
	public void RemoveObject(String Type,int id,PrintWriter out)
	{
		RemoveObject(Type,id,out,false);
	}
	
	public void RemoveObject(String Type,int id,PrintWriter out,boolean fromAPI)
	{
		try
		{
			boolean bSetStatus=false;
			String remcat="";
			System.out.println("RemoveObject Type="+Type+" id="+id);
			if (Type.equalsIgnoreCase("lifecycle")) Type="domain";
			if (Type.equalsIgnoreCase("release")) Type="application";
			if (Type.equalsIgnoreCase("relversion")) Type="application";
			if (Type.equalsIgnoreCase("compversion")) Type="component";
			if (Type.equalsIgnoreCase("builder")) Type="buildengine";
   
			String AccessColumn;
			if (Type.equalsIgnoreCase("component")) {
				AccessColumn="compid";
			}
			else
			if (Type.equalsIgnoreCase("environment")) {
				AccessColumn="envid";
			}
			else
			if (Type.equalsIgnoreCase("CREDENTIALS")) {
				AccessColumn="credid";
			}
			else
			if (Type.equalsIgnoreCase("application")) {
				AccessColumn="appid";
			}
			else
			if (Type.equalsIgnoreCase("buildengine")) {
				AccessColumn="builderid";
			}
			else
			{
				AccessColumn=Type+"id";
			}
			String AccessTable="dm.dm_"+Type+"access";
			if (Type.equalsIgnoreCase("domain")) {
				// Check to see if the domain is empty and we have delete permission
				if (domainHasObjects(id)) {
					// Domain is not empty
					out.print("{\"error\" : \"" + getAssociatedMsg() + "\", \"success\" : false}");
					return;
				}
				if (domainReferencesAnyObject(id))
				 bSetStatus=true;
			}
			else
			if (Type.equalsIgnoreCase("environment")) {
				if (envIsReferenced(id)) {
					bSetStatus=true;
				}
				// Remove any servers associated with this environment
				DeleteFromTable("dm.dm_environmentvars","envid",id);
				DeleteFromTable("dm.dm_serversinenv","envid",id);
				DeleteFromTable("dm.dm_appsallowedinenv","envid",id);
				DeleteFromTable("dm.dm_appsinenv","envid",id);
			}
			else
			if (Type.equalsIgnoreCase("user")) {
				if (userIsReferenced(id)) {
					bSetStatus=true;
				}
			}
			else
			if (Type.equalsIgnoreCase("usergroup")) {
				DeleteFromTable("dm.dm_privileges","groupid",id);
				if (userGroupIsReferenced(id)) {
					bSetStatus=true;
				}
			}
			else
			if (Type.equalsIgnoreCase("component")) {
				if (componentIsReferenced(id,false,false)) {
					bSetStatus=true;
				}
			}
			else
			if (Type.equalsIgnoreCase("application")) {
				DeleteFromTable("dm.dm_appsallowedinenv","appid",id);
				DeleteFromTable("dm.dm_appsinenv","appid",id);
				if (applicationIsReferenced(id)) {
					bSetStatus=true;
				}
			}
			else
			if (Type.equalsIgnoreCase("repository")) {
				if (repositoryIsReferenced(id)) {
					bSetStatus=true;
				}
			}
			else
			if (Type.equalsIgnoreCase("credentials")) {
				if (credentialIsReferenced(id)) {
					out.print("{\"error\" : \"Cannot delete the Credential at this time since it is in use.\", \"success\" : false}");
					return;
				}
			}
			else
			if (Type.equalsIgnoreCase("server")) {
				if (serverIsReferenced(id)) {
					bSetStatus=true;
				}
			}		
			else
			if (Type.equalsIgnoreCase("procedure"))
			{
				if (procfuncIsReferenced(id)) { 
					out.print("{\"error\" : \"Cannot delete the Procedure at this time since it is in use.\", \"success\" : false}");
					return;
				} 
			}
			else
			if (Type.equalsIgnoreCase("function"))
			{
				if (procfuncIsReferenced(id)) { 
					out.print("{\"error\" : \"Cannot delete the Function at this time since it is in use.\", \"success\" : false}");
					return;
				} 
			}
			else
			if (Type.equalsIgnoreCase("action"))
			{
				if (actionIsReferenced(id)) {
					out.print("{\"error\" : \"Cannot delete this Action - it has been used in a deployment.\", \"success\" : false}");
					return;
				}
			}
			else
			if (Type.equalsIgnoreCase("notify"))
			{
				if (notifierIsReferenced(id)) {
					out.print("{\"error\" : \"Cannot delete this Notifier - one or more of its associated templates are in use.\", \"success\" : false}");
					return;
				}
			}
			else
			if (Type.equalsIgnoreCase("template"))
			{       
				if (templateIsReferenced(id)) {
					out.print("{\"error\" : \"Cannot delete this Template - it is in use.\", \"success\" : false}");
					return; 
				}       
			}
			else
			if (Type.equalsIgnoreCase("datasource"))
			{       
		        if (datasourceIsReferenced(id)) {
		                out.print("{\"error\": \"Cannot delete this Datasource - it is in use.\", \"success\" : false}");
		                return; 
		        }       
			} 

			if (Type.equalsIgnoreCase("procedure") || Type.equalsIgnoreCase("function"))
			{
				Type="action";
				AccessTable = "dm.dm_actionaccess";
				AccessColumn = "actionid";
			}

			if (Type.equalsIgnoreCase("ServerCompType"))
			{
				if (comptypeIsReferenced(id)) { 
					out.print("{\"error\" : \"Cannot delete the Component Type at this time since it is in use.\", \"success\" : false}");
					return;
				} 
				Type="type";
				AccessTable = "dm.dm_typeaccess";
				AccessColumn = "comptypeid";
			}
   
			int res=0;
			// Delete from the access control table
			if (!Type.equalsIgnoreCase("template") && !Type.equalsIgnoreCase("user") && !Type.equalsIgnoreCase("usergroup")) 
			{
				DeleteFromTable(AccessTable,AccessColumn,id);
			}
			if (Type.equalsIgnoreCase("action")) {
				// Remove this action from the fragments table(if referenced)
				Action action = getAction(id,true);
				int t=(action.getKind() == ActionKind.GRAPHICAL)?2:3;
				String pf=(action.getKind() == ActionKind.GRAPHICAL)?"cy":"cp";
				Category cat = action.getCategory();
				int domainid = action.getDomainId();
				res = DeleteFromTable("dm.dm_fragments","actionid",id);
				if (res == 1) {
					// Remove from fragments failed for "actionid" - must be "functionid"
					res = DeleteFromTable("dm.dm_fragments","functionid",id);
				}
				res = DeleteFromTable("dm.dm_fragmentattrs","typeid",id);
				res = DeleteFromTable("dm.dm_actionarg","actionid",id);
				res = DeleteFromTable("dm.dm_actionflows","actionid",id);
				res = DeleteFromTable("dm.dm_actionfrags","actionid",id);
				res = DeleteFromTable("dm.dm_action_categories","id",id);
				
				if (cat != null) {
					boolean removeCategory = !CategoryInDomain(id, cat.getId(), domainid, t);
					if (removeCategory) {
						remcat = pf+cat.getId()+"-"+domainid;
					}
				}
			}
			// If the object is referenced and we have to update the status, do it here
			System.out.println("Type=["+Type+"]");
			if (bSetStatus) {
				System.out.println("UPDATE dm.dm_"+Type+" SET status='D' WHERE id="+id);
				PreparedStatement stmt = getDBConnection().prepareStatement("UPDATE dm.dm_"+Type+" SET status='D' WHERE id=?");
				stmt.setInt(1,id);
				stmt.execute();
				res = (stmt.getUpdateCount()<1)?1:0;
				stmt.close();
			} else {
				if (Type.equalsIgnoreCase("component")) {
					// Deleting a component permanently - get rid of any variables (if we just mark it as deleted
					// then we'll leave the variables where they are for reference
					Component comp = getComponent(id,true);
					Category cat = comp.getCategory();
					int domainid = comp.getDomainId();
					if (comp.getParentId()==0) {
						// This is a BASE version - delete all the children
						List<Component> children = comp.getVersions();
						for (Component cc: children) {
							DeleteFromTable("dm.dm_componentvars","compid",cc.getId());
							DeleteFromTableWhere("dm.dm_compitemprops","compitemid in (select id from dm.dm_componentitem where compid=?)",cc.getId());
							DeleteFromTable("dm.dm_componentitem","compid",cc.getId());
							DeleteFromTable("dm.dm_component_categories","id",cc.getId());
							DeleteFromTable("dm.dm_buildhistory","compid",cc.getId());
							DeleteFromTable("dm.dm_component","id",cc.getId());
						}
					}
					DeleteFromTable("dm.dm_componentvars","compid",id);
					DeleteFromTableWhere("dm.dm_compitemprops","compitemid in (select id from dm.dm_componentitem where compid=?)",id);
					DeleteFromTable("dm.dm_componentitem","compid",id);
					DeleteFromTable("dm.dm_component_categories","id",id);
					DeleteFromTable("dm.dm_buildhistory","compid",id);
					DeleteFromTable("dm.dm_component","id",id);
					if (cat != null) {
						boolean removeCategory = !CategoryInDomain(id, cat.getId(), domainid, 1);
						if (removeCategory) {
							remcat = "cc"+cat.getId()+"-"+domainid;
						}
					}
				}
				else
				if (Type.equalsIgnoreCase("repository")) {
					DeleteFromTable("dm.dm_repositorytextpattern","repositoryid",id);
					DeleteFromTable("dm.dm_repositoryaccess","repositoryid",id);
					DeleteFromTable("dm.dm_repositoryprops","repositoryid",id);
					DeleteFromTable("dm.dm_repositoryvars","repositoryid",id);
					DeleteFromTable("dm.dm_repositorytextpattern","repositoryid",id);
					res = DeleteFromTable("dm.dm_"+Type,"id",id);
				}
				else
				if (Type.equalsIgnoreCase("application")) {
					DeleteFromTable("dm.dm_applicationaccess","appid",id);
					DeleteFromTable("dm.dm_applicationcomponent","appid",id);
					DeleteFromTable("dm.dm_applicationvars","appid",id);
					DeleteFromTable("dm.dm_approval","appid",id);
					//DeleteFromTable("dm.dm_appsallowedinenv","appid",id);
					DeleteFromTable("dm.dm_appsinenv","appid",id);
					res = DeleteFromTable("dm.dm_"+Type,"id",id);
				}
				else
				if (Type.equalsIgnoreCase("datasource")) {
					DeleteFromTable("dm.dm_datasourceaccess","datasourceid",id);
					DeleteFromTable("dm.dm_datasourceprops","datasourceid",id);
					DeleteFromTable("dm.dm_datasourcevars","datasourceid",id);
					DeleteFromTable("dm.dm_field","datasourceid",id);
					res = DeleteFromTable("dm.dm_"+Type,"id",id);
				}
				else
				if (Type.equalsIgnoreCase("server")) {
					DeleteFromTable("dm.dm_servervars","serverid",id);
					DeleteFromTable("dm.dm_serveraccess","serverid",id);
					DeleteFromTable("dm.dm_servercomptype","serverid",id);
					DeleteFromTable("dm.dm_serversinenv","serverid",id);
					DeleteFromTable("dm.dm_serverstatus","serverid",id);
					res = DeleteFromTable("dm.dm_"+Type,"id",id);
				}
				else
				if (Type.equalsIgnoreCase("notify")) {
					DeleteFromTable("dm.dm_notifyaccess","notifyid",id);
					DeleteFromTable("dm.dm_notifyprops","notifyid",id);
					DeleteFromTable("dm.dm_template","notifierid",id);
					res = DeleteFromTable("dm.dm_"+Type,"id",id);
				}
				else
				{
					// All other object types
					System.out.println("remove default");
					res = DeleteFromTable("dm.dm_"+Type,"id",id);
				}
			}		
			getDBConnection().commit();
			if (fromAPI) {
				out.print("{\"success\" : true}");
			} else {
				out.print("{\"error\" : \"\", \"success\" : true, \"remcat\": \""+remcat+"\"}");
			}
			return;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return;
	}
	
	private String MoveObjectByTable(String table,int id,int TargetDomain)
	{
		try
		{
			System.out.println("MoveObjectByTable table="+table+" id="+id+" TargetDomain="+TargetDomain);
			
			PreparedStatement stmt = getDBConnection().prepareStatement("UPDATE dm."+table+" SET domainid=? WHERE id=?");
			stmt.setInt(1,TargetDomain);
			stmt.setInt(2,id);
			stmt.execute();
			String res=(stmt.getUpdateCount()<1)?"Failed to update "+table+" domain":"";
			stmt.close();
			getDBConnection().commit();
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return "";	// success
	}
	
	private String MoveTemplate(int id,int TargetNotifier)
	{
		try
		{
			System.out.println("MoveTemplate id="+id+" TargetNotifier="+TargetNotifier);
			
			PreparedStatement stmt = getDBConnection().prepareStatement("UPDATE dm.dm_template SET notifierid=? WHERE id=?");
			stmt.setInt(1,TargetNotifier);
			stmt.setInt(2,id);
			stmt.execute();
			String res=(stmt.getUpdateCount()<1)?"Failed to update notification template":"";
			stmt.close();
			getDBConnection().commit();
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return "";	// success
	}
	
	private String MoveServerToNewEnvironment(int SourceEnvironment,int id,int TargetEnvironment)
	{
		try
		{
			System.out.println("MoveServerToNewEnvironment origenv="+SourceEnvironment+" id="+id+" TargetEnvironment="+TargetEnvironment);
			//
			// Check if the server is already associated with the target environment. If so, reject the move
			//
			String res = "";
			PreparedStatement ck = getDBConnection().prepareStatement("SELECT count(*) FROM dm.dm_serversinenv WHERE serverid=? AND envid=?");
			ck.setInt(1,id);
			ck.setInt(2,TargetEnvironment);
			ResultSet rs = ck.executeQuery();
			if (rs.next()) {
				if (rs.getInt(1)>0) res="Server is already associated with target environment";
			}
			rs.close();
			ck.close();
			if (res.length()==0) {
				PreparedStatement stmt = getDBConnection().prepareStatement("UPDATE dm.dm_serversinenv SET envid=? WHERE serverid=? AND envid=?");
				stmt.setInt(1,TargetEnvironment);
				stmt.setInt(2,id);
				stmt.setInt(3,SourceEnvironment);
				stmt.execute();
				res=(stmt.getUpdateCount()<1)?"Failed to update dm_serversinenv domain":"";
				stmt.close();
				if (res.length()==0) {
					PreparedStatement stmt2 = getDBConnection().prepareStatement("UPDATE dm.dm_environment SET modified=?,modifierid=? WHERE id IN (?,?)");
					stmt2.setLong(1,timeNow());
					stmt2.setInt(2,getUserID());
					stmt2.setInt(3,TargetEnvironment);
					stmt2.setInt(4,SourceEnvironment);
					stmt2.execute();
					// Now record the removal and addition for each Environment
					Environment tgtenv = getEnvironment(TargetEnvironment,false);
					Environment srcenv = getEnvironment(SourceEnvironment,false);
					Server server = getServer(id,false);
					String linkval="<a href='javascript:SwitchDisplay(\"se"+server.getId()+"\");'><b>"+server.getName()+"</b></a>";
					String linkval2="<a href='javascript:SwitchDisplay(\"en"+srcenv.getId()+"\");'><b>"+srcenv.getName()+"</b></a>";
					String linkval3="<a href='javascript:SwitchDisplay(\"en"+tgtenv.getId()+"\");'><b>"+tgtenv.getName()+"</b></a>";
					RecordObjectUpdate(tgtenv, "Server "+linkval+" Added to Environment",id);
					RecordObjectUpdate(srcenv, "Server "+linkval+" Removed from Environment",id);
					RecordObjectUpdate(server, "Moved from Environment "+linkval2+" to Environment "+linkval3,TargetEnvironment);

				}
				getDBConnection().commit();
			}
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return "";	// success
	}
	
	private String MoveUser(int id,int TargetDomain)
	{
		User user = getUser(id);
		Domain domain = getDomain(TargetDomain);
		RecordObjectUpdate(user,"Moved from domain "+user.getDomain().getName()+" to domain "+domain.getName());
		return MoveObjectByTable("dm_user",id,TargetDomain);
	}
	
	private String MoveGroup(int id,int TargetDomain)
	{
		UserGroup group = getGroup(id);
		Domain domain = getDomain(TargetDomain);
		RecordObjectUpdate(group,"Moved from domain "+group.getDomain().getName()+" to domain "+domain.getName());
		return MoveObjectByTable("dm_usergroup",id,TargetDomain);
	}
	
	private String MoveDomain(int id,int TargetDomain)
	{
		return MoveObjectByTable("dm_domain",id,TargetDomain);
	}
	
	private String MoveAction(int id,int TargetDomain)
	{
		Action action = getAction(id,false);
		Domain domain = getDomain(TargetDomain);
		RecordObjectUpdate(action,"Moved from domain "+action.getDomain().getName()+" to domain "+domain.getName());
		return MoveObjectByTable("dm_action",id,TargetDomain);
	}
	
	private String MoveComponent(int id,int TargetDomain)
	{
		String errtext = VerifyCompTargetDomain(id,TargetDomain);
		if (errtext != null && errtext != "") return "Cannot move Component: "+errtext+" is not availble in target domain";
		try
		{
			System.out.println("MoveComponent id="+id+" TargetDomain="+TargetDomain);
			
			PreparedStatement stmt = getDBConnection().prepareStatement("UPDATE dm.dm_component SET domainid=? WHERE id=? OR parentid=?");
			stmt.setInt(1,TargetDomain);
			stmt.setInt(2,id);
			stmt.setInt(3,id);
			stmt.execute();
			String res=(stmt.getUpdateCount()<1)?"Failed to update component domain":"";
			stmt.close();
			if (res=="") {
				Component comp = getComponent(id,false);
				Domain domain = getDomain(TargetDomain);
				RecordObjectUpdate(comp,"Moved from domain "+comp.getDomain().getName()+" to domain "+domain.getName());
			}
			getDBConnection().commit();
			return res;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return "";	// success
		
	}
	
	private String MoveEnvironment(int id,int TargetDomain)
	{
		Environment env = getEnvironment(id,false);
		Domain domain = getDomain(TargetDomain);
		RecordObjectUpdate(env,"Moved from domain "+env.getDomain().getName()+" to domain "+domain.getName());
		return MoveObjectByTable("dm_environment",id,TargetDomain);
	}
	
	private String MoveServer(int id,int TargetDomain)
	{
		Server server = getServer(id,false);
		Domain domain = getDomain(TargetDomain);
		RecordObjectUpdate(server,"Moved from domain "+server.getDomain().getName()+" to domain "+domain.getName());
		return MoveObjectByTable("dm_server",id,TargetDomain);
	}
	
	private String MoveNotify(int id,int TargetDomain)
	{
		return MoveObjectByTable("dm_notify",id,TargetDomain);
	}
	
	private String MoveRepository(int id,int TargetDomain)
	{
		return MoveObjectByTable("dm_repository",id,TargetDomain);
	}
	
	private String MoveDatasource(int id,int TargetDomain)
	{
		return MoveObjectByTable("dm_datasource",id,TargetDomain);
	}
	
	private String MoveCredentials(int id,int TargetDomain)
	{
		return MoveObjectByTable("dm_credentials",id,TargetDomain);
	}
	
	private String MoveCompType(int id,int TargetDomain)
	{
		return MoveObjectByTable("dm_type",id,TargetDomain);
	}
	
	
	
	public String MoveObject(String ObjectType,int parentid, int id,String TargetObject,int targetid)
	{
		if (ObjectType.equalsIgnoreCase("lifecycle")) ObjectType="domain";
		if (TargetObject.equalsIgnoreCase("lifecycle")) TargetObject="domain";
		if (TargetObject.equalsIgnoreCase("domain")) {
			// Check we have update permission to the target domain
			Domain domain = getDomain(targetid);
			if (!domain.isUpdatable()) return "Cannot Move "+ObjectType+": Permission Denied";
		}
		System.out.println("MoveObject ObjectType="+ObjectType+" id="+id+" TargetObject="+TargetObject+" targetid="+targetid+" parentid="+parentid);
		if (ObjectType.equalsIgnoreCase("user") && TargetObject.equalsIgnoreCase("domain")) return MoveUser(id,targetid);
		if (ObjectType.equalsIgnoreCase("domain") && TargetObject.equalsIgnoreCase("domain")) return MoveDomain(id,targetid);
		if (ObjectType.equalsIgnoreCase("action") && TargetObject.equalsIgnoreCase("domain")) return MoveAction(id,targetid);
		if (ObjectType.equalsIgnoreCase("procedure") && TargetObject.equalsIgnoreCase("domain")) return MoveAction(id,targetid);
		if (ObjectType.equalsIgnoreCase("function") && TargetObject.equalsIgnoreCase("domain")) return MoveAction(id,targetid);
		if (ObjectType.equalsIgnoreCase("component") && TargetObject.equalsIgnoreCase("domain")) return MoveComponent(id,targetid);
		if (ObjectType.equalsIgnoreCase("environment") && TargetObject.equalsIgnoreCase("domain")) return MoveEnvironment(id,targetid);
		if (ObjectType.equalsIgnoreCase("usergroup") && TargetObject.equalsIgnoreCase("domain")) return MoveGroup(id,targetid);
		if (ObjectType.equalsIgnoreCase("server") && TargetObject.equalsIgnoreCase("domain")) return MoveServer(id,targetid);
		if (ObjectType.equalsIgnoreCase("server") && TargetObject.equalsIgnoreCase("environment")) return MoveServerToNewEnvironment(parentid,id,targetid);
		if (ObjectType.equalsIgnoreCase("notify") && TargetObject.equalsIgnoreCase("domain")) return MoveNotify(id,targetid);
		if (ObjectType.equalsIgnoreCase("template") && TargetObject.equalsIgnoreCase("notify")) return MoveTemplate(id,targetid);
		if (ObjectType.equalsIgnoreCase("repository") && TargetObject.equalsIgnoreCase("domain")) return MoveRepository(id,targetid);
		if (ObjectType.equalsIgnoreCase("datasource") && TargetObject.equalsIgnoreCase("domain")) return MoveDatasource(id,targetid);
		if (ObjectType.equalsIgnoreCase("credentials") && TargetObject.equalsIgnoreCase("domain")) return MoveCredentials(id,targetid);
		if (ObjectType.equalsIgnoreCase("servercomptype") && TargetObject.equalsIgnoreCase("domain")) return MoveCompType(id,targetid);
		
		return "Not yet implemented";
	}
	
	public int RenameObject(String Type,int id,String NewName)
	{
		if (Type.equalsIgnoreCase("template")) {
			//
			// Renaming a template - these don't have domains but are children of notify objects (which do).
			// So the name should be unique within a notify object
			//
			int Exists=0;
			try
			{
				PreparedStatement st1 = getDBConnection().prepareStatement("SELECT notifierid FROM dm.dm_template where id=?");
				st1.setInt(1, id);
				ResultSet rs1 = st1.executeQuery();
				if (rs1.next()) {
					// notifier found
					PreparedStatement st = getDBConnection().prepareStatement("SELECT id,name FROM dm.dm_template where name=? AND id<>? AND notifierid=?");
					st.setString(1,NewName);
					st.setInt(2,id);
					st.setInt(3,rs1.getInt(1));
					ResultSet rs = st.executeQuery();
					if (rs.next()) {
						Exists=1;
					}
					rs.close();
					st.close();
					if (Exists == 0) {
						//
						// template name doesn't exist in the same notifier - rename it
						//
						PreparedStatement st2 = getDBConnection().prepareStatement("UPDATE dm.dm_template SET name=? WHERE id=?");
						st2.setString(1,NewName);
						st2.setInt(2,id);
						st2.execute();
						st2.close();
						getDBConnection().commit();
					}
				}
				rs1.close();
				st1.close();
				return Exists;
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				rollback();
			}
		}
		else
		{
			//
			// First check that the new name doesn't clash with one already in this domain
			//
			int Exists=0;
			try
			{
				System.out.println("RenameObject Type="+Type+" NewName="+NewName);
				if (Type.equalsIgnoreCase("lifecycle")) Type="domain";
				if (Type.equalsIgnoreCase("appversion")) Type="application";
				if (Type.equalsIgnoreCase("release")) Type="application";
    if (Type.equalsIgnoreCase("procedure")) Type="action";
    if (Type.equalsIgnoreCase("function")) Type="action";
    
				PreparedStatement st1 = getDBConnection().prepareStatement("SELECT domainid FROM dm.dm_"+Type+" where id=?");
				st1.setInt(1, id);
				ResultSet rs1 = st1.executeQuery();
				if (rs1.next()) {
					// id is valid and we have its domain
					PreparedStatement st = getDBConnection().prepareStatement("SELECT id,name FROM dm.dm_"+Type+" where name=? AND id<>? AND domainid=?");
					st.setString(1,NewName);
					st.setInt(2,id);
					st.setInt(3,rs1.getInt(1));
					ResultSet rs = st.executeQuery();
					if (rs.next()) {
						Exists=1;
					}
					rs.close();
					st.close();
					if (Exists == 0) {
						//
						// doesn't exist in one of our domains - rename it
						//
						PreparedStatement st2 = getDBConnection().prepareStatement("UPDATE dm.dm_"+Type+" SET name=? WHERE id=?");
						st2.setString(1,NewName);
						st2.setInt(2,id);
						st2.execute();
						st2.close();
						getDBConnection().commit();
					}
				}
				rs1.close();
				st1.close();
				return Exists;
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				rollback();
			}
		}
		return 0;	// success
	}

 public boolean CheckIfObjectExists(String Type,int id,String NewName)
 {
  //
  // First check that the new name doesn't clash with one already in this domain
  //
  boolean Exists=false;
  try
  {
   System.out.println("CheckIfObjectExists Type="+Type+" NewName="+NewName);
   if (Type.equalsIgnoreCase("lifecycle")) Type="domain";
   PreparedStatement st1 = getDBConnection().prepareStatement("SELECT domainid FROM dm.dm_"+Type+" where id=?");
   st1.setInt(1, id);
   ResultSet rs1 = st1.executeQuery();
   if (rs1.next()) {
    // id is valid and we have its domain
    PreparedStatement st = getDBConnection().prepareStatement("SELECT id,name FROM dm.dm_"+Type+" where name=? AND id<>? AND domainid=? AND status<>'D'");
    st.setString(1,NewName);
    st.setInt(2,id);
    st.setInt(3,rs1.getInt(1));
    ResultSet rs = st.executeQuery();
    if (rs.next()) {
     Exists=true;
    }
    rs.close();
    st.close(); 
   }
   rs1.close();
   st1.close();
   return Exists;
  }
  catch (SQLException e)
  {
   e.printStackTrace();
   rollback();
  }
  return false; // success
 }

 	public String GetUserDateFormat()
 	{
 		return (m_datefmt==null)?m_defaultdatefmt:m_datefmt;
 	}
 	
	public String GetUserTimeFormat()
	{
		return (m_timefmt==null)?m_defaulttimefmt:m_timefmt;
	}
	
	public User getUser(int userid) {
		// Specific user
		if (m_userhash != null && (System.currentTimeMillis() - m_userhashCreationTime) > 2000) {
			// User hash is over 2 seconds old. Delete it. We only need this for caching of
			// user when we're making the same call to getUser in rapid succession.
			m_userhash.clear();
			m_userhash = null;
		}
		 
		if (m_userhash == null) {
			  m_userhash = new Hashtable<Integer,User>();
			  m_userhashCreationTime = System.currentTimeMillis();
		}
		User cached = m_userhash.get(userid);
		if (cached != null) {
			return cached;	// already found this domain previously
		}
		
		String sql = "SELECT u.name, u.domainid, u.email, u.realname, "
				+ "  u.phone, u.locked, u.forcechange, u.lastlogin, u.status, "
				+ "  u.datefmt, u.timefmt, u.datasourceid, "
				+ "  uc.id, uc.name, uc.realname, u.created, "
				+ "  um.id, um.name, um.realname, u.modified "
				//+ "  ,uo.id, uo.name, uo.realname, g.id, g.name "
				+ "FROM dm.dm_user u "
				+ "LEFT OUTER JOIN dm.dm_user uc ON u.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON u.modifierid = um.id "		// modifier
				//+ "LEFT OUTER JOIN dm.dm_user uo ON u.ownerid = uo.id "		// owner user
				//+ "LEFT OUTER JOIN dm.dm_usergroup g ON u.ogrpid = g.id "		// owner group
				+ "WHERE u.id=?";
		try
		{
			if (userid < 0)
			{
			 User ret = new User(this,-1,"","");
			 ret.setEmail("");
			 ret.setPhone("");
			 ret.setAccountLocked(false);
			 ret.setForceChangePass(false);
			 return ret;
			}
			
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, userid);
			ResultSet rs = stmt.executeQuery();
			User ret = null;
			if(rs.next()) {
				int domainid = rs.getInt(2);
				if(ValidDomain(domainid)) {
					ret = new User(this, userid, rs.getString(1), rs.getString(4));
					ret.setDomainId(domainid);
					ret.setEmail(rs.getString(3));
					ret.setPhone(rs.getString(5));
					ret.setAccountLocked(getBoolean(rs, 6, false));
					ret.setForceChangePass(getBoolean(rs, 7, false));
					java.sql.Timestamp lastLogin = rs.getTimestamp(8);
					
					if(lastLogin != null) {
						ret.setLastLogin((int) (lastLogin.getTime()/1000));
					}
					getStatus(rs, 9, ret);
					ret.setDateFmt(rs.getString(10));
					ret.setTimeFmt(rs.getString(11));
					int dsid = getInteger(rs,12,0);
					if (dsid>0) {
						Datasource ds = this.getDatasource(dsid,true);
						ret.setDatasource(ds);
					}
					// ret.setDomain(getDomainName(domainid));
					getCreatorModifier(rs, 13, ret);
				}
			}
			rs.close();
			m_userhash.put(userid,ret);
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve user from database");
	}
	
	
	public boolean updateUser(User user, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_user ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case USER_REALNAME:  update.add(", realname = ?", changes.get(field)); break;
			case USER_EMAIL:	 update.add(", email = ?", changes.get(field)); break;
			case USER_PHONE:	 update.add(", phone = ?", changes.get(field)); break;
			case USER_PASSWORD: {
				String hash = encryptPassword((String) changes.get(field));
				update.add(", passhash = ?", hash);
				}
				break;
			case USER_LOCKED:	 update.add(", locked = ?", changes.getBoolean(field)); break;
			case USER_CHNG_PASS: update.add(", forcechange = ?", changes.getBoolean(field)); break;		
			case USER_DATE_FMT:  
				update.add(", datefmt = ?",changes.get(field));
				m_datefmt = (String) changes.get(field);
				System.out.println("m_datefmt now "+m_datefmt);
				break;
			case USER_TIME_FMT: 
				update.add(", timefmt = ?",changes.get(field));
				m_timefmt = (String) changes.get(field);
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(user, update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", user.getId());
		
		try {
			update.execute();
			RecordObjectUpdate(user,changes);
			getDBConnection().commit();
			update.close();
			m_userhash.remove(user.getId());	// in case it's cached.
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	private UserGroupList getAssociatedGroups(int uid,boolean withuser)
	{
		UserGroupList ret = new UserGroupList();
		String sql;
		if (withuser) {
			sql = "select a.id,a.name,a.email,a.domainid from dm.dm_usergroup a,dm.dm_usersingroup b where b.userid=? and a.id=b.groupid and a.status='N' order by a.name";
		} else {
			if (m_OverrideAccessControl) {
				sql = "select a.id,a.name,a.email,a.domainid from dm.dm_usergroup a where a.domainid in ("+m_domainlist+") and a.id not in (select b.groupid from dm.dm_usersingroup b where b.userid=?) and a.status='N' order by a.name";
			} else {
				// "normal" user - should only display groups to which we have access
				sql = 		"select a.id,a.name,a.email,a.domainid from dm.dm_usergroup a "
						+	"where a.domainid in ("+m_domainlist+") "
						+ 	"and a.id not in (select b.groupid from dm.dm_usersingroup b where b.userid=?) "
						+	"and a.id in (select c.groupid from dm.dm_usersingroup c where c.userid="+getUserID()+") "
						+	"and a.status='N'"
						+	"order by a.name";
			}
		}
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, uid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				// System.out.println("got usergroup="+rs.getString(2));
				int gid = rs.getInt(1);
				if(gid != UserGroup.EVERYONE_ID) {
					UserGroup group = new UserGroup(this,gid,rs.getString(2));
					group.setEmail(rs.getString(4));
					ret.add(group);
				}
			}
			rs.close();
			System.out.println("returning ret size="+ret.size());
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve groups for user from database");
	}
	
	public UserGroupList getGroupsForUser(int uid)
	{
		return getAssociatedGroups(uid,true);
	}
	
	public UserGroupList getGroupsNotForUser(int uid)
	{
		return getAssociatedGroups(uid,false);
	}
	
	// This is only used for access control checking - so we add automatic membership of the EVERYONE group here
	public UserGroupList getGroupsForCurrentUser()
	{
		UserGroupList ret = getAssociatedGroups(getUserID(), true);
		if(ret == null) {
			ret = new UserGroupList();
		}
		ret.add(UserGroup.EVERYONE);
		return ret;
	}
	
	public UserGroupList getGroupsForTask(int tid,boolean withtask)
	{
		UserGroupList ret = new UserGroupList();
		String sql;
		if (withtask) {
			sql = "select a.id,a.name,a.email,a.domainid from dm.dm_usergroup a,dm.dm_taskaccess b where b.taskid=? and a.id=b.usrgrpid";
		} else {
			sql = "select a.id,a.name,a.email,a.domainid from dm.dm_usergroup a where a.domainid in ("+m_domainlist+") and a.id not in (select b.usrgrpid from dm.dm_taskaccess b where b.taskid=?)";
		}
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, tid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				// System.out.println("got group="+rs.getString(2));
				UserGroup group = new UserGroup(this,rs.getInt(1),rs.getString(2));
				group.setEmail(rs.getString(4));
				ret.add(group);
			}
			rs.close();
			System.out.println("returning ret size="+ret.size());
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve groups for task from database");
	}
	
	public void AddGroupToTask(int taskid,int groupid)
	{
		String csql = "select count(*) from dm.dm_taskaccess where taskid=? and usrgrpid=?";
		String sql= "insert into dm.dm_taskaccess(taskid,usrgrpid) values(?,?)";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(csql);
			stmt.setInt(1, taskid);
			stmt.setInt(2, groupid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				if (rs.getInt(1)==0) {
					PreparedStatement stmt2 = getDBConnection().prepareStatement(sql);
					stmt2.setInt(1, taskid);
					stmt2.setInt(2, groupid);
					stmt2.execute();
					stmt2.close();
				}
			}
			stmt.close();
			getDBConnection().commit();
			return;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to update groups for task in database");
	}
	
	public void RemoveGroupFromTask(int taskid,int groupid)
	{
		String sql= "delete from dm.dm_taskaccess where taskid=? and usrgrpid=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, taskid);
			stmt.setInt(2, groupid);
			stmt.execute();
			stmt.close();
			getDBConnection().commit();
			return;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to remove groups for task in database");
	}
	
	public UserGroup getGroup(int groupid) {
		String sql = "SELECT g.name, g.domainid, g.email, g.summary, g.status, "
				+ "  g.acloverride, g.tabendpoints, g.tabapplications, g.tabactions, g.tabproviders, g.tabusers,"
				+ "  uc.id, uc.name, uc.realname, g.created, "
				+ "  um.id, um.name, um.realname, g.modified, "
				+ "  uo.id, uo.name, uo.realname, go.id, go.name "
				+ "FROM dm.dm_usergroup g "
				+ "LEFT OUTER JOIN dm.dm_user uc ON g.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON g.modifierid = um.id "		// modifier
				+ "LEFT OUTER JOIN dm.dm_user uo ON g.ownerid = uo.id "			// owner user
				+ "LEFT OUTER JOIN dm.dm_usergroup go ON g.ogrpid = go.id "		// owner group
				+ "WHERE g.id=?";
		try
		{
			if (groupid < 0) {
				UserGroup ret = new UserGroup(this,-1,"");
				ret.setName("");
				ret.setEmail("");
				ret.setSummary("");
				return ret;
			}
   
			UserGroup ret = null;
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, groupid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				int domainid = rs.getInt(2);
				// PAG mod - checking ValidDomain causes issues when selecting groups that are defined
				// against higher level domains then our "home" domain. Just comment out for now. We
				// may need to have this call optional
				// if (ValidDomain(domainid))
				// {
					ret = new UserGroup(this, groupid, rs.getString(1));
					ret.setDomainId(domainid);
					ret.setEmail(rs.getString(3));
					ret.setSummary(rs.getString(4));
					getStatus(rs, 5, ret);
					ret.setAclOverride(getBoolean(rs,6,false));
					ret.setTabEnd(getBoolean(rs,7,false));
					ret.setTabApp(getBoolean(rs,8,false));
					ret.setTabAction(getBoolean(rs,9,false));
					ret.setTabProv(getBoolean(rs,10,false));
					ret.setTabUser(getBoolean(rs,11,false));
					getCreatorModifierOwner(rs, 12, ret);
				// 	}
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve user from database");
	}
	
	
	public boolean updateGroup(UserGroup group, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_usergroup ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case GROUP_EMAIL:	update.add(", email = ?", changes.get(field)); break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(group, update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", group.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}

	
	public List<UserGroup> getGroups(Integer userid) {
		String sql="select id,name,domainid,email from dm.dm_usergroup where status<> 'D' domainid in ("+m_domainlist+")";
		if (userid > 0)
		{
			sql=sql+ " and id in (select groupid from dm.dm_usersingroup where userid="+userid+")";
		}
		try
		{
			UserGroupList ret = new UserGroupList();
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				int domainid = rs.getInt(3);
				if (ValidDomain(domainid))
				{
					UserGroup group = new UserGroup(this,rs.getInt(1),rs.getString(2));
					group.setEmail(rs.getString(4));
					// group.setDomain(getDomainName(domainid));
					ret.add(group);
				}
			}
			rs.close();
			ret.sort();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve usergroup from database");
	}
	
	public List<UserGroup> getGroups() {
		return getGroups(0);
	}
	
	private void getGroupsInDomain(List<UserGroup> ret,int domainid) {
		try
		{
			System.out.println("getGroupsInDomain("+domainid+")");
			PreparedStatement stmt = getDBConnection().prepareStatement("select id,name,email from dm.dm_usergroup where domainid=?");
			stmt.setInt(1, domainid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				System.out.println("adding usergroup "+rs.getString(2));
				UserGroup group = new UserGroup(this,rs.getInt(1),rs.getString(2));
				group.setEmail(rs.getString(3));
				ret.add(group);
			}
			rs.close();
			//
			// Now recurse upwards to grab the usergroups in the parent domains
			//
			Domain domain = getDomain(domainid);
			if (domain != null && domain.getDomainId() > 0) {
				getGroupsInDomain(ret,domain.getDomainId());
			}
			return;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve usergroup from database");
	}
	
	public List<UserGroup> getGroupsForDomain(int domainid) {
		// Returns a list of user groups associated with this domain and all the parent domains
		List <UserGroup> ret = new ArrayList<UserGroup>();
		getGroupsInDomain(ret,domainid);
		return ret;
	}
	
		
	
	private UserList getAssociatedUsers(int gid,boolean ingroup)
	{
		UserList ret = new UserList();
		String sql;
		if (gid == 1) {
			if (ingroup) {
				// Everyone - just list all users in our domain list
				sql = "select a.id,a.name,a.realname,a.email,a.domainid from dm.dm_user a where a.domainid in ("+m_domainlist+") order by a.name";
			} else {
				sql = null;	// No one is NOT in  the "Everyone" group
			}
		} else {
			if (ingroup) {
				sql = "select a.id,a.name,a.realname,a.email,a.domainid from dm.dm_user a,dm.dm_usersingroup b where b.groupid=? and a.id=b.userid order by a.name";
			} else {
				sql = "select a.id,a.name,a.realname,a.email,a.domainid from dm.dm_user a where a.domainid in ("+m_domainlist+") and a.id not in (select b.userid from dm.dm_usersingroup b where b.groupid=?) order by a.name";
			}
		}
		System.out.println("sql="+sql);
		System.out.println("gid="+gid);
		try
		{
			if (sql != null) {
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				if (gid != 1) stmt.setInt(1, gid);
				ResultSet rs = stmt.executeQuery();
				while (rs.next())
				{
					User user = new User();
					user.setEmail(rs.getString(4));
					user.setId(rs.getInt(1));
					user.setName(rs.getString(2));;
					user.setRealName(rs.getString(3));
					ret.add(user);
				}
				rs.close();
			}
			System.out.println("returning ret size="+ret.size());
			ret.sort();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve users in group from database");
	}
	
	public UserList getUsersInGroup(int gid) {
		return getAssociatedUsers(gid,true);
	}
	
	public UserList getUsersNotInGroup(int gid) {
		return getAssociatedUsers(gid,false);
	}
	
	public int AddUserToGroup(int gid,int uid)
	{
		System.out.println("AddUserToGroup, gid="+gid+" uid="+uid);
		String sql="insert into dm.dm_usersingroup(userid,groupid) values(?,?)";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, uid);
			stmt.setInt(2, gid);
			stmt.execute();
			stmt.close();
			UserGroup grp = getGroup(gid);
			User user = getUser(uid);
			RecordObjectUpdate(grp,"Added user "+user.getName()+" to Group");
			RecordObjectUpdate(user,"Added to Group "+grp.getName());
			getDBConnection().commit();
			return 0;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to add user to group in database");
	}

	public int RemoveUserFromGroup(int gid,int uid)
	{
		System.out.println("RemoveUserFromGroup, gid="+gid+" uid"+uid);
		String sql="delete from dm.dm_usersingroup where userid=? and groupid=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, uid);
			stmt.setInt(2, gid);
			stmt.execute();
			stmt.close();
			UserGroup grp = getGroup(gid);
			User user = getUser(uid);
			RecordObjectUpdate(grp,"Removed user "+user.getName()+" from Group");
			RecordObjectUpdate(user,"Removed from Group "+grp.getName());
			getDBConnection().commit();
			return 0;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to remove user from group in database");
	}
	
	public String getBuildLog(int jobid,int buildid)
	{
		String sql1="select	a.value,a.encrypted,b.id	"
				+	"from	dm.dm_buildengineprops	a,	"
				+	"	dm.dm_buildjob		b	"
				+	"where	a.builderid=b.builderid	"
				+	"and	b.id=?";
		try {
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,jobid);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				String serverurl = rs1.getString(1);
				String encrypted = rs1.getString(2);
				int buildjobid=rs1.getInt(3);
				if (encrypted != null && encrypted.equalsIgnoreCase("y")) {
					serverurl = new String(Decrypt3DES(serverurl, m_passphrase));
				}
				BuildJob buildjob = getBuildJob(buildjobid);
				Builder engine = getBuilder(buildjob.getBuilderId());
				Credential cred = engine.getCredential();
				// http://localhost:8081/job/IT%20Guys/142/consoleText
				String res = getJSONFromServer(serverurl+"/job/"+buildjob.getProjectName().replaceAll(" ", "%20")+"/"+buildid+"/consoleText",cred);
				System.out.println("got console output:");
				System.out.println(res);
				return res;
			}
			rs1.close();
			stmt1.close();
		} catch(SQLException ex) {
			
		}
		return null;
	}
	
	public int getBuildTime(int jobid,int buildid)
	{		
		BuildJob buildjob = getBuildJob(jobid);
		Builder builder = getBuilder(buildjob.getBuilderId());
		Credential cred = builder.getCredential();
		String server = getBuildServerURL(buildjob.getBuilderId());
		String project = buildjob.getProjectName().replaceAll(" ", "%20");
		String url = server + "/job/"+project+"/"+buildid+"/api/json";
		System.out.println("getBuildTime, url="+url);
		String res = getJSONFromServer(url, cred);
		int timestamp=0;
		try {
			JsonObject buildObject = new JsonParser().parse(res).getAsJsonObject();
			timestamp = (int)(buildObject.get("timestamp").getAsLong() / 1000);
			System.out.println("result is "+timestamp);
		} catch(JsonSyntaxException ex) {
			System.out.println("JSON syntax exception:");
			System.out.println(res);
		}
		return timestamp;
	}
	
	public JSONObject getBuildDetails(int jobid,int buildid)
	{
		JSONObject ret = new JSONObject();
		BuildJob buildjob = getBuildJob(jobid);
		ret.add("buildjob",buildjob.getLinkJSON());
		Builder builder = getBuilder(buildjob.getBuilderId());
		ret.add("builder", builder.getLinkJSON());
		ret.add("commit",buildjob.getBuildCommitID(buildid));
		ret.add("timestamp",formatDateToUserLocale(buildjob.getBuildTime(buildid)));
		ret.add("duration", buildjob.getBuildDuration(buildid));
		return ret;
	}
	
	private String getBuildServerURL(int builderid)
	{
		String sql1="select value,encrypted from dm.dm_buildengineprops where name='Server URL' and builderid = ?";
		String server=null;
		try {
			PreparedStatement stmt1=getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,builderid);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				server = rs1.getString(1);
				String encrypted = getString(rs1,2,"N");
				if (encrypted.equalsIgnoreCase("y")) {
					server = new String(Decrypt3DES(server,m_passphrase));
				}
			}
			rs1.close();
			stmt1.close();
		} catch (SQLException ex) {
			
		}
		return server;
	}
	
	public int getBuildDuration(int jobid,int buildid)
	{
		BuildJob buildjob = getBuildJob(jobid);
		Builder builder = getBuilder(buildjob.getBuilderId());
		Credential cred = builder.getCredential();
		String server = getBuildServerURL(buildjob.getBuilderId());
		String project = buildjob.getProjectName().replaceAll(" ", "%20");
		String url = server + "/job/"+project+"/"+buildid+"/api/json";
		System.out.println("getBuildDuration, url="+url);
		String res = getJSONFromServer(url, cred);
		int duration=0;
		try {
			JsonObject buildObject = new JsonParser().parse(res).getAsJsonObject();
			duration = buildObject.get("duration").getAsInt();
			System.out.println("result is "+duration);
		} catch(JsonSyntaxException ex) {
			System.out.println("JSON syntax exception:");
			System.out.println(res);
		}
		return duration;
	}
	
	public String getBuildCommitID(int jobid,int buildid)
	{
		String ret = "";
		String sql = "SELECT commit FROM dm.dm_buildhistory WHERE buildjobid=? AND buildnumber=?";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,jobid);
			stmt.setInt(2,buildid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				// Should only be one row
				ret = rs.getString(1);
			}
			rs.close();
			stmt.close();
		} catch(SQLException ex) {
			System.out.println("Failed to get commit id from build("+jobid+","+buildid+") - "+ex.getMessage());
			ex.printStackTrace();
		}
		return ret;
	}
	
	public JSONArray getBuildFiles(int jobid,int buildid)
	{
		System.out.println("getBuildFiles("+jobid+","+buildid+")");
		JSONArray ret = new JSONArray();
		String sql = "select filename from dm.dm_buildfiles where buildnumber=? and buildjobid=?";
		try {
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql);
			stmt1.setInt(1,buildid);
			stmt1.setInt(2,jobid);
			ResultSet rs1 = stmt1.executeQuery();
			while (rs1.next()) {
				ret.add(rs1.getString(1));
			}
			rs1.close();
			stmt1.close();
			return ret;
		} catch(SQLException ex) {
			
		}
		return null;
	}
	
 
 public ArrayList<String> getBuildFilesList(int jobid,int buildid)
 {
  System.out.println("getBuildFiles("+jobid+","+buildid+")");
  ArrayList<String> ret = new ArrayList<String>();
  String sql = "select filename from dm.dm_buildfiles where buildnumber=? and buildjobid=?";
  try {
   PreparedStatement stmt1 = getDBConnection().prepareStatement(sql);
   stmt1.setInt(1,buildid);
   stmt1.setInt(2,jobid);
   ResultSet rs1 = stmt1.executeQuery();
   while (rs1.next()) {
    ret.add(rs1.getString(1));
   }
   rs1.close();
   stmt1.close();
   return ret;
  } catch(SQLException ex) {
   
  }
  return ret;
 }
 

 
	public JSONArray getBuildTargets(int jobid,int buildid)
	{
		System.out.println("getBuildTargetsForJob("+jobid+","+buildid+")");
		JSONArray ret = new JSONArray();
		String sql1 = "select	a.id,a.name,a.parentid,b.deploymentid,c.id,c.name,coalesce(d.started,0)	"
				+	"from	dm.dm_component	a	"
				+	"left outer join dm.dm_compsonserv b on b.buildnumber=? and a.id=b.compid	"
				+	"left outer join dm.dm_server c on c.id=b.serverid	"
				+	"left outer join dm.dm_deployment d on d.deploymentid=b.deploymentid "
				+	"where	a.buildjobid=? and b.deploymentid is not null order by 7 desc";
		try {
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,buildid);
			stmt1.setInt(2,jobid);
			ResultSet rs1 = stmt1.executeQuery();
			while (rs1.next()) {
				JSONObject rowobj = new JSONObject();
				JSONObject compobj = new JSONObject();
				compobj.add("id",rs1.getInt(1));
				compobj.add("name", rs1.getString(2));
				compobj.add("type",getInteger(rs1,3,0)>0?"cv":"co");
				JSONObject servobj = new JSONObject();
				servobj.add("id",rs1.getInt(5));
				servobj.add("name",rs1.getString(6));
				rowobj.add("component", compobj);
				rowobj.add("server",servobj);
				rowobj.add("deploymentid", rs1.getInt(4));
				int dt = getInteger(rs1,7,0);
				if (dt>0) {
					rowobj.add("dt",formatDateToUserLocale(dt));
				} else {
					rowobj.add("dt","");
				}
				ret.add(rowobj);
			}
			rs1.close();
			stmt1.close();
			return ret;
		} catch(SQLException ex) {
			
		}
		return null;
	}
	
	public JSONArray recordJenkinsBuild(String encodedurl) throws Exception
	{
		JSONArray ret = new JSONArray();
		System.out.println("recordJenkinsBuild encodedurl=["+encodedurl+"]");
    	try {
			String buildurl = java.net.URLDecoder.decode(encodedurl, "UTF-8");
			System.out.println("buildurl=["+buildurl+"]");
			int jns = buildurl.lastIndexOf("/job/")+5;	// Job Name Start
	    	String jobname = buildurl.substring(jns,buildurl.indexOf("/",jns));
	    	System.out.println("jobname=["+jobname+"]");
	    	int bns = buildurl.lastIndexOf('/',buildurl.length()-2);
	    	int buildno = Integer.parseInt(buildurl.substring(bns+1,buildurl.length()-1));
	    	System.out.println("build number="+buildno);
	    	int sp=(buildurl.startsWith("http://"))?7:8;	
	    	String server = buildurl.substring(0,buildurl.indexOf('/',sp));
	    	System.out.println("Jenkins Server=["+server+"]");
	    	//
	    	// Note, this just updates the builds for the latest version of any component
	    	// with the specified build job. The Jenkins plug-in may need to pass Branch
	    	// along to make sure this works with the latest version of a component on the
	    	// Branch. Worry about this later
	    	//
	    	System.out.println("Looking for Build Engine with Server URL or Jenkins Match URL of ["+server+"]");
	    	String sql1 = 	"select a.id,a.name,b.value,b.encrypted,c.id,c.name,d.id	"	+
					"from 	dm.dm_buildjob	a,			"	+
					"		dm.dm_buildengineprops b,	"	+
					"		dm.dm_component c,			"	+
					"		dm.dm_buildengine d			"	+
					"where	a.projectname=?				"	+
					"and	a.builderid=b.builderid		"	+
					"and	b.name='Server URL'			"	+
					"and	c.buildjobid = a.id			"	+
					"and	d.id = a.builderid			"	+
					"and	c.status='N'				"	+
					"and    not exists (select x.id from dm.dm_component x where x.predecessorid = c.id and x.status='N') " +
					"and	d.domainid in ("+m_domainlist+")";
	    	String sql2 = "INSERT INTO dm.dm_buildhistory(buildjobid,buildnumber,compid,userid,timestamp,success) VALUES(?,?,?,?,?,?)";
	    	String sql3 = "UPDATE dm.dm_component SET lastbuildnumber=? WHERE id=?";
	    	String sql4 = "SELECT value,encrypted FROM dm.dm_buildengineprops WHERE name='Jenkins Match URL' AND builderid=?";
	    	PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
	    	stmt1.setString(1,jobname);
	    	ResultSet rs1 = stmt1.executeQuery();
	    	while (rs1.next()) {
	    		int buildjobid = rs1.getInt(1);
	    		// String buildjobname = rs1.getString(2);
	    		String serverurl = rs1.getString(3);
	    		String encrypted = rs1.getString(4);
	    		int compid = rs1.getInt(5);
	    		// String compname = rs1.getString(6);
	    		int buildengineid = rs1.getInt(7);
	    		
	    		boolean match=false;
	    		if (encrypted != null && encrypted.equalsIgnoreCase("y")) {
	    			// This server URL is encrypted
	    			byte[] sun = Decrypt3DES(serverurl,m_passphrase);
	    			serverurl = new String(Decrypt3DES(serverurl, m_passphrase));
	    			String comparevalue = new String(sun).replaceAll("/*$","");	// remove any trailing / chars;
	    			System.out.println("Comparing ["+server+"] with ["+comparevalue+"] (was encrypted)");
	    			match = comparevalue.equalsIgnoreCase(server);
	    		} else {
	    			// Server URL is not encrypted - straight forward compare
	    			String comparevalue = serverurl.replaceAll("/*$","");
	    			System.out.println("Comparing ["+server+"] with ["+comparevalue+"]");
	    			match = comparevalue.equalsIgnoreCase(server);
	    		}
	    		System.out.println("\"Server URL\" match="+match);
	    		
	    		if (!match) {
	    			// No match yet - check if there's a Jenkins Match URL to compare instead
	    			PreparedStatement stmt4 = m_conn.prepareStatement(sql4);
	    			stmt4.setInt(1,buildengineid);
	    			ResultSet rs4 = stmt4.executeQuery();
	    			if (rs4.next()) {
	    				System.out.println("Jenkins Match URL seen");
	    				String matchURL = rs4.getString(1);
	    				String enc = rs4.getString(2);
	    				if (enc != null && enc.equalsIgnoreCase("y")) {
	    					// This matchURL address is encrypted
	    					System.out.println("matchURL is encrypted");
	    					matchURL = new String(Decrypt3DES(matchURL, m_passphrase));
	    				}
	    				matchURL = matchURL.replaceAll("/*$","");	// get rid of any trailing / chars
	    				System.out.println("Comparing ["+server+"] with ["+matchURL+"]");
	    				match = matchURL.equalsIgnoreCase(server);
	    				if (match) {
	    					// Replace the server part in the encoded URL with our Server URL for
	    					// the subsequent ping back to Jenkins. Format of encodedurl is:
	    					// http[s]://server:port/blah/job/<project>/<buildno>
	    					// Format of matchURL will be
	    					// http[s]://server:port/blah
	    					int soj = encodedurl.indexOf("/job/");
	    					encodedurl = serverurl.replaceAll("/*$", "")+encodedurl.substring(soj);
	    					System.out.println("encodedurl now ["+encodedurl+"] (for callback)");
	    				}
	    			}
		    		rs4.close();
		    		stmt4.close();
		    		System.out.println("\"Jenkins Match URL\" match="+match);
		    	}
	    		if (match) {
	    			// Found a matching build job for this server URL
	    			// Ping Jenkins back and see if this build was successful or not.
	    			Builder builder = getBuilder(buildengineid);
	    			Credential cred = builder.getCredential();
	    			String res = getJSONFromServer(serverurl+"/api/json",cred);
					System.out.println("Found a match - build job "+buildjobid+" component "+compid);
					System.out.println("result from Jenkins is "+res);
					JsonObject issueObject = new JsonParser().parse(res).getAsJsonObject();
					String result = issueObject.get("result").getAsString();
					System.out.println("result is "+result);
					boolean success = result.equalsIgnoreCase("success");
					PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
					stmt2.setInt(1,buildjobid);
					stmt2.setInt(2,buildno);
					stmt2.setInt(3,compid);
					stmt2.setInt(4, getUserID());
					stmt2.setLong(5,timeNow());
					stmt2.setString(6,success?"Y":"N");
					stmt2.execute();
					stmt2.close();
					if (success) {
						// Update "lastbuildnumber" for component
						PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
						stmt3.setInt(1,buildno);
						stmt3.setInt(2,compid);
						stmt3.execute();
						stmt3.close();
					}
					//
					// Return a list of Components associated with this build job
					//
					JSONObject tbobj = new JSONObject();
		    		Component comp = getComponent(compid,true);		
		    		BuildJob buildjob = getBuildJob(buildjobid);
		    		tbobj.add("component", comp.getLinkJSON());
		    		tbobj.add("buildengine", builder.getLinkJSON());
		    		tbobj.add("buildjob", buildjob.getLinkJSON());
					ret.add(tbobj);
	    		} else {
                    System.out.println("no match");
	    		}
	    	}
	    	rs1.close();
	    	stmt1.close();
	    	System.out.println("end of sql1 loop");
	    	getDBConnection().commit();
	    	return ret;	
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			throw new Exception("Unsupported Encoding Exception: URL="+encodedurl);
		} catch (NumberFormatException e) {
			throw new Exception("Invalid Build number");
		} catch (SQLException ex) {
			ex.printStackTrace();
			rollback();
		}
		return null;
	}
	
	public void AddBuildToComponent(Component comp,int buildno,String [] files,String commit,boolean success)
	{
		String sql1="SELECT count(*) FROM dm.dm_buildhistory WHERE compid=? AND buildnumber=?";
		String sql2="UPDATE dm.dm_buildhistory SET buildjobid=?, commit=?, userid=?, timestamp=?, success=? WHERE compid=? AND buildnumber=?";
		String sql3="INSERT INTO dm.dm_buildhistory(buildjobid,buildnumber,commit,compid,userid,timestamp,success) VALUES(?,?,?,?,?,?,?)";
		String sql4="UPDATE dm.dm_component SET lastbuildnumber=? WHERE id=?";
		String sql5="DELETE FROM dm.dm_buildfiles WHERE buildjobid=? AND buildnumber=?";
		String sql7="INSERT INTO dm.dm_buildfiles(buildjobid,buildnumber,filename) VALUES(?,?,?)";
	
		BuildJob buildjob = comp.getBuildJob();
		if (buildjob == null) return;	// Already checked for this in the API code - belt and braces
		try {
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,comp.getId());
			stmt1.setInt(2,buildno);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				if (rs1.getInt(1)==0) {
					// First time for this build number against this component
					PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
					stmt3.setInt(1,buildjob.getId());
					stmt3.setInt(2,buildno);
					if (commit == null) {
						stmt3.setNull(3,Types.CHAR);
					} else {
						stmt3.setString(3,commit);
					}
					stmt3.setInt(4,comp.getId());
					stmt3.setInt(5,getUserID());
					stmt3.setLong(6,timeNow());
					stmt3.setString(7,success?"Y":"N");
					stmt3.execute();
					stmt3.close();
				} else {
					// This build number has already been associated with this component
					PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
					stmt2.setInt(1,buildjob.getId());
					stmt2.setString(2,commit);
					stmt2.setInt(3,getUserID());
					stmt2.setLong(4,timeNow());
					stmt2.setString(5,success?"Y":"N");
					stmt2.setInt(6,comp.getId());
					stmt2.setInt(7,buildno);
					stmt2.execute();
					stmt2.close();
				}
				// Add the defects and files for this build
				PreparedStatement stmt5 = getDBConnection().prepareStatement(sql5);
				PreparedStatement stmt7 = getDBConnection().prepareStatement(sql7);
				stmt5.setInt(1,buildjob.getId());
				stmt5.setInt(2,buildno);
				stmt5.execute();
				stmt7.setInt(1,buildjob.getId());
				stmt7.setInt(2,buildno);
				for (int i=0;i<files.length;i++) {
					stmt7.setString(3,files[i]);
					stmt7.execute();
				}

				if (success) {
					System.out.println("Updating lastbuildnumber to "+buildno+" for component "+comp.getId());
					PreparedStatement stmt4 = getDBConnection().prepareStatement(sql4);
					stmt4.setInt(1,buildno);
					stmt4.setInt(2,comp.getId());
					stmt4.execute();
					stmt4.close();
				}
				getDBConnection().commit();
			}
			rs1.close();
			stmt1.close();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
	}
	
	
	
	public boolean domainHasObjects(int domainid) {
		System.out.println("in domainHasObjects("+domainid+")");
		// Returns true if domain has objects has associated with it, false otherwise
		boolean res=false;
		String sql[] = {
				"SELECT count(*) FROM dm.dm_environment WHERE domainid=? and status <> 'D'",
				"SELECT count(*) FROM dm.dm_domain WHERE domainid=? and status <> 'D'",
				"SELECT count(*) FROM dm.dm_user WHERE domainid=? and status <> 'D'",
				"SELECT count(*) FROM dm.dm_credentials WHERE domainid=? and status <> 'D'",
				"SELECT count(*) FROM dm.dm_application WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_action WHERE domainid=? and status <> 'D' and graphical = 'Y'",
    "SELECT count(*) FROM dm.dm_action WHERE domainid=? and status <> 'D' and function = 'Y'",
    "SELECT count(*) FROM dm.dm_action WHERE domainid=? and status <> 'D' and function <> 'Y' and graphical <> 'Y'",       
				"SELECT count(*) FROM dm.dm_usergroup WHERE domainid=? and status <> 'D'",
	   "SELECT count(*) FROM dm.dm_datasource WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_repository WHERE domainid=? and status <> 'D'"
		};
		String msg[] = {
		  "Environments",
		  "Domains",
		  "Users",
		  "Credentials",
		  "Applications",
		  "Actions",
    "Functions",
    "Procedures",    
		  "User Groups",
		  "Data Sources",
    "Repositories"
		};
		
		try
		{
		 AssociatedMsg = "The following are associated to the Domain:,";
		 
			for (int i=0;i<sql.length;i++) {
				PreparedStatement stmt = getDBConnection().prepareStatement(sql[i]);
				stmt.setInt(1,domainid);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					System.out.println(sql[i]+" = "+ rs.getInt(1));
					if (rs.getInt(1)>0) {
						res=true;
						AssociatedMsg += rs.getInt(1) + " " + msg[i] + ",";
					}
					rs.close();
				} else {
					res=true;	// failsafe (if we get here, no rows were retrieved)
				}
				stmt.close();
			}
			return res;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve domain objects for domain " + domainid);
	}

 public boolean objHasChildren(TreeObject node, String typestr) 
 {
  int id = node.getId();
  int i = 0;
  if (id ==9)
   id = 9;
 
  // Returns true if object has children, false otherwise
  boolean res=false;
  String sql[] = {
    "SELECT count(*) FROM dm.dm_serversinenv a, dm.dm_server b WHERE envid=? and b.status <> 'D' and a.serverid = b.id",
    "SELECT count(*) FROM dm.dm_domain WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_application a WHERE a.parentid=? and a.status <> 'D' AND a.domainid=(select b.domainid from dm.dm_application b where b.id=a.parentid)",
    "SELECT count(*) FROM dm.dm_component WHERE parentid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_action WHERE parentid=? AND status = 'A'",
    "SELECT count(*) FROM dm.dm_buildjob WHERE builderid=? AND status = 'N'"
  };
  
  String sql2[] = {
    "SELECT count(*) FROM dm.dm_environment WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_user WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_credentials WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_application WHERE domainid=? and status <> 'D' and isRelease <> 'Y'",
    "SELECT count(*) FROM dm.dm_action WHERE domainid=? and status <> 'D' and graphical = 'Y'",
    "SELECT count(*) FROM dm.dm_usergroup WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_datasource WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_server WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_notify WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_action WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_component WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_credentials WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_repository WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_application WHERE domainid=? and status <> 'D' and isRelease = 'Y'",
    "SELECT count(*) FROM dm.dm_action WHERE domainid=? and status <> 'D' and graphical <> 'Y'",
    "SELECT count(*) FROM dm.dm_type WHERE domainid=? and status <> 'D'",
    "SELECT count(*) FROM dm.dm_buildengine WHERE domainid=? and status <> 'D'"
  };
  
  System.out.println("objhaschildren, ObjectType="+node.GetObjectType()+" id="+id);

  if (node.GetObjectType() == ObjectType.ENVIRONMENT)
   i = 0;
  else if (node.GetObjectType() == ObjectType.DOMAIN)
   i = 1;
  else if (node.GetObjectType() == ObjectType.APPLICATION || node.GetObjectType() == ObjectType.RELEASE)
   i = 2;
  else if (node.GetObjectType() == ObjectType.COMPONENT)
   i = 3;
  else if (node.GetObjectType() == ObjectType.ACTION)
   i = 4;
  else if (node.GetObjectType() == ObjectType.FUNCTION)
   i = 4;
  else if (node.GetObjectType() == ObjectType.PROCEDURE)
   i = 4;
  else if (node.GetObjectType() == ObjectType.BUILDER)
   i = 5;
  else if (node.GetObjectType() == ObjectType.FRAGMENT)
   return false;
  else 
   return true;
  
  try
  {   
	  System.out.println("sql["+i+"]="+sql[i]);
    PreparedStatement stmt = getDBConnection().prepareStatement(sql[i]);
    stmt.setInt(1,id);
    ResultSet rs = stmt.executeQuery();
    if (rs.next()) 
    {
    	System.out.println("count = "+rs.getInt(1));
     if (rs.getInt(1)>0) 
      res=true;
     
     rs.close();
    }
    stmt.close();
    
    System.out.println("res="+res);
    
   if (node.GetObjectType() == ObjectType.DOMAIN && res == false)
   {
    System.out.println("TYPE="+typestr);
    if (typestr.equalsIgnoreCase("environments"))
     i=0;
    else if (typestr.equalsIgnoreCase("users"))
     i=1;
    else if (typestr.equalsIgnoreCase("credentials"))
     i=2;
    else if (typestr.equalsIgnoreCase("applications"))
     i=3;
    else if (typestr.equalsIgnoreCase("actions"))
     i=4;
    else if (typestr.equalsIgnoreCase("groups"))
     i=5;
    else if (typestr.equalsIgnoreCase("datasources"))
     i=6;
    else if (typestr.equalsIgnoreCase("servers"))
     i=7;
    else if (typestr.equalsIgnoreCase("notifiers"))
     i=8;
    else if (typestr.equalsIgnoreCase("action"))
     i=9;
    else if (typestr.equalsIgnoreCase("components"))
     i=10;
    else if (typestr.equalsIgnoreCase("credentials"))
     i=11;
    else if (typestr.equalsIgnoreCase("repositories"))
     i=12;
    else if (typestr.equalsIgnoreCase("releases"))
     i=13;
    else if (typestr.equalsIgnoreCase("functions"))
     i=14;    
    else if (typestr.equalsIgnoreCase("procedures"))
     i=14; 
    else if (typestr.equalsIgnoreCase("types"))
     i=15;  
    else if (typestr.equalsIgnoreCase("builders"))
     i=16;
    else
     return res;
    
    stmt = getDBConnection().prepareStatement(sql2[i]);
    stmt.setInt(1,id);
    rs = stmt.executeQuery();
    if (rs.next()) 
    {
     if (rs.getInt(1)>0) 
      res=true;
     
     rs.close();
    }
    stmt.close();    
   }
   return res;
  }
  catch(SQLException e)
  {
   e.printStackTrace();
   rollback();
  }
  return res;
 }
 
 public boolean domainReferencesAnyObject(int domainid) {
  System.out.println("in domainHasObjects("+domainid+")");
  // Returns true if domain has objects has associated with it, false otherwise
  boolean res=false;
  String sql[] = {
    "SELECT count(*) FROM dm.dm_environment WHERE domainid=?",
    "SELECT count(*) FROM dm.dm_domain WHERE domainid=?",
    "SELECT count(*) FROM dm.dm_user WHERE domainid=?",
    "SELECT count(*) FROM dm.dm_credentials WHERE domainid=?",
    "SELECT count(*) FROM dm.dm_application WHERE domainid=?",
    "SELECT count(*) FROM dm.dm_action WHERE domainid=?",
    "SELECT count(*) FROM dm.dm_usergroup WHERE domainid=?",
    "SELECT count(*) FROM dm.dm_datasource WHERE domainid=?"
  };
  
  try
  {   
   for (int i=0;i<sql.length;i++) {
    PreparedStatement stmt = getDBConnection().prepareStatement(sql[i]);
    stmt.setInt(1,domainid);
    ResultSet rs = stmt.executeQuery();
    if (rs.next()) {
     System.out.println(sql[i]+" = "+ rs.getInt(1));
     if (rs.getInt(1)>0) {
      res=true;
      break;
     }
     rs.close();
    } else {
     res=true; // failsafe (if we get here, no rows were retrieved)
    }
    stmt.close();
   }
   return res;
  }
  catch(SQLException e)
  {
   e.printStackTrace();
   rollback();
  }
  return res;
 }

 
	public String getAssociatedMsg()
 {
  return AssociatedMsg;
 }

 public void setAssociatedMsg(String associatedMsg)
 {
  AssociatedMsg = associatedMsg;
 }

 public Domain getDomain(int domainid) {
	 try {
		 if (domainid < 0) {
			 Domain ret = new Domain(this, 0, "");
			 ret.setName("");
			 ret.setSummary("");
			 return ret;
		}
		 
		if (m_domainhash != null && (System.currentTimeMillis() - m_domainhashCreationTime) > 2000) {
			// Domain hash is over 2 seconds old. Delete it. We only need this for caching of
			// domains when we're making the same call to getDomain in rapid succession when
			// populating fully qualified domains in drop-down lists of actions (for example).
			m_domainhash.clear();
			m_domainhash = null;
		}
		 
		if (m_domainhash == null) {
			  m_domainhash = new Hashtable<Integer,Domain>();
			  m_domainhashCreationTime = System.currentTimeMillis();
		}
		Domain ret = m_domainhash.get(domainid);
		if (ret != null) return ret;	// already found this domain previously
		
		PreparedStatement stmt = getDBConnection().prepareStatement(
			"SELECT d.name, d.domainid, d.summary, d.lifecycle, "
			+ "  uc.id, uc.name, uc.realname, d.created, "
			+ "  um.id, um.name, um.realname, d.modified, "
			+ "  uo.id, uo.name, uo.realname, g.id, g.name, "
			+ "	 x.id, x.name, x.hostname "
			+ "FROM dm.dm_domain d "
			+ "LEFT OUTER JOIN dm.dm_user uc ON d.creatorid = uc.id "		// creator
			+ "LEFT OUTER JOIN dm.dm_user um ON d.modifierid = um.id "		// modifier
			+ "LEFT OUTER JOIN dm.dm_user uo ON d.ownerid = uo.id "			// owner user
			+ "LEFT OUTER JOIN dm.dm_usergroup g ON d.ogrpid = g.id "		// owner group
			+ "LEFT OUTER JOIN dm.dm_engine x ON x.domainid = d.id WHERE d.id=?");
		stmt.setInt(1, domainid);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			int parentdomainid = getInteger(rs, 2, 0);	
			if ((parentdomainid == 0) || ValidDomain(parentdomainid, true)) {	// RHT 14/02/2014 - this stops Global being an invalid parent!!!
				ret = new Domain(this, domainid, rs.getString(1));
				ret.setDomainId(parentdomainid);
				ret.setSummary(rs.getString(3));
				ret.setLifecycle(getBoolean(rs,4,false));
				if(parentdomainid != 0) {
					ret.setParentDomain(getDomainName(parentdomainid));
				}
				getCreatorModifierOwner(rs, 5, ret);	
				int engineid = getInteger(rs, 18, 0);
				if(engineid != 0) {
					Engine eng = new Engine(this, engineid, rs.getString(19));
					eng.setHostname(rs.getString(20));
					ret.setEngine(eng);
				} else {
					Engine eng = ret.findNearestEngine(); 
					if (eng == null) {
						eng = new Engine(this, 0, "");
						eng.setHostname("");
					} 
					ret.setEngine(eng);					 
				}
			} else {
				System.out.println(parentdomainid + " is not a valid domain for user " + getUserID());
			}
		}
		rs.close();
		if (ret != null) {
			m_domainhash.put(domainid,ret);	// save for subsequent retrieval
		}
		return ret;
	}
	catch(SQLException e) {
		e.printStackTrace();
		rollback();
	}
	throw new RuntimeException("Unable to retrieve domain " + domainid + " from database");
}

	 
	public boolean updateDomain(Domain dom, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_domain ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case DOMAIN_LIFECYCLE: {
				Boolean lifecycle = (Boolean) changes.get(field);
				update.add(", lifecycle = ?", ((lifecycle != null) && lifecycle.booleanValue()) ? "Y" : "N");
				}
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(dom,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", dom.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			m_domainhash.remove(dom.getId());	// in case it's cached.
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public void updateDomainOrder(String [] domorder) {
		String sql="UPDATE dm.dm_domain SET position=? WHERE id=?";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			for (int i=0;i<domorder.length;i++) {
				System.out.println("UPDATE dm.dm_domain SET position="+i+" WHERE id="+domorder[i]);	
				stmt.setInt(1,i);
				stmt.setInt(2,Integer.parseInt(domorder[i]));
				stmt.execute();
				System.out.println("UPDATE COUNT = "+stmt.getUpdateCount());
			}
			getDBConnection().commit();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		
	}

	public List<TreeObject> getDomains(Integer domainid) {
		return getTreeObjects(ObjectType.DOMAIN,domainid,-1);
	}
	
	public List<Domain> getChildDomains(Domain tdomain) {
		int domainid = tdomain.getId();
		
		if (m_cdhash != null && (System.currentTimeMillis() - m_cdhashCreationTime) > 2000) {
			// Child Domain hash is over 2 seconds old. Delete it. We only need this for caching of
			// child domains when we're making the same call to getChildDomains in rapid succession.
			m_cdhash.clear();
			m_cdhash = null;
		}
		 
		if (m_cdhash == null) {
			  m_cdhash = new Hashtable<Integer,List<Domain>>();
			  m_cdhashCreationTime = System.currentTimeMillis();
		}
		List<Domain> cached = m_cdhash.get(domainid);
		if (cached != null) {
			return cached;	// already found this domain previously
		}
		
		
		String sql="select id,name,domainid,ownerid,ogrpid from dm.dm_domain where domainid="+domainid+" order by position";
		try
		{
			System.out.println("cd) sql="+sql);
			List <Domain> ret = new ArrayList<Domain>();
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				Domain domain = new Domain();
				int parentdomainid = rs.getInt(3);
				if (parentdomainid == 0 || ValidDomain(parentdomainid,true))
				{
					domain.setId(rs.getInt(1));
					domain.setName(rs.getString(2));
					int ownerid = rs.getInt(4);
					if (rs.wasNull()) {
						int groupid = rs.getInt(5);
						if (!rs.wasNull()) {
							UserGroup group = getGroup(groupid);
							domain.setOwner(group);
						}
					} else {
						User owner = getUser(ownerid);
						domain.setOwner(owner);
					}
					domain.setParentDomain(getDomainName(parentdomainid));
					ret.add(domain);
				}
				else
				{
					System.out.println("Not a valid parentdomain "+parentdomainid);
				}
			}
			rs.close();
			m_cdhash.put(domainid,ret);
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve usergroup from database");
	}
	
	// RHT - gm has already been calculated and will include Everyone for us
	private Hashtable<Integer,String> DomainViewAccess(int domainID, List<Integer> gm) throws SQLException
	{
//		System.out.println("DomainViewAccess(" + domainID + ")");
		Hashtable<Integer,String> res = new Hashtable<Integer,String>();
		Hashtable<Integer, ObjectAccess> da = getAccessForDomain(domainID);
		// debug
		//for(Integer gid : da.keySet()) {
		//	System.out.println("da[" + gid + "] = " + da.get(gid));
		//}
		// end-debug
		
		//PreparedStatement stmt = m_conn.prepareStatement("SELECT groupid FROM dm.dm_usersingroup WHERE userid=?");
		//stmt.setInt(1, m_userID);
		//ResultSet rs = stmt.executeQuery();
		//while (rs.next()) {
		//	for (Integer domaingroupid : da.keySet()) {
		//	    if (domaingroupid == rs.getInt(1))  //<=== This won't work!!!
		//	    {
		//	    	ObjectAccess oa = da.get(domaingroupid);
		//	    	if (oa.isViewable()) {
		//	    		res.put(domaingroupid,"Y");
		//	    	}
		//	    }
		//	}
		//}
		for(Integer gid : gm) {
			ObjectAccess oa = da.get(gid);
			if((oa != null) && oa.isViewable()) {
				res.put(gid,"Y");
			}
		}
		//rs.close();
		//stmt.close();
		
		// debug
		//StringBuffer temp = new StringBuffer();
		//for(Integer gid : res.keySet()) {
		//	if(temp.length() > 0) temp.append(", ");
		//	temp.append(gid + "=" + res.get(gid));
		//}
		//System.out.println("DomainViewAccess returning: " + temp);
		// end-debug
		return res;
	}

private List<Integer> GetGroupMembership()
{
	// Returns a list of usergroup ids of which the user is a member
	List<Integer> res = new ArrayList<Integer>();
	res.add(UserGroup.EVERYONE_ID);	// Everyone group - implicit membership
	try
	{
		PreparedStatement stmt = getDBConnection().prepareStatement("SELECT groupid FROM dm.dm_usersingroup WHERE userid=?");
		stmt.setInt(1,getUserID());
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			res.add(rs.getInt(1));
		}
		rs.close();
		stmt.close();
		return res;
		
	}
	catch (SQLException ex)
	{
		ex.printStackTrace();
		rollback();
	}
	throw new RuntimeException("Unable to retrieve user groups from database");
}

public List<TreeObject> getTreeObjects(ObjectType ot, int domainID, int catid)
{
	System.out.println("getTreeObjects, ot="+ot);
 List<TreeObject> ret = new ArrayList<TreeObject>();
 String sql=null;
 PreparedStatement stmt = null;
 String sql_access="";
 String sql_hasaccess="";
 boolean newQuery=false;
 try {
  List<Integer> gm = GetGroupMembership();
  Hashtable<Integer,String> vag = DomainViewAccess((domainID > 0) ? domainID : m_userDomain, gm);   // view access groups
  Hashtable<Integer,String> vagc = new Hashtable<Integer,String>(); // This will take a copy of the access groups
  switch (ot) {
  case APPLICATION:
   sql = "select a.id, a.name from dm.dm_application a where a.domainid=? and a.predecessorid is null and a.status='N' and a.isRelease <> 'Y'";
   sql_access = "select count(*) from dm.dm_applicationaccess where appid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_applicationaccess where appid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case RELEASE:
   sql = "select a.id, a.name from dm.dm_application a where a.domainid=? and a.predecessorid is null and a.status='N' and a.isRelease='Y'";
   sql_access = "select count(*) from dm.dm_applicationaccess where appid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_applicationaccess where appid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;   
  case RELVERSION:
   sql = "select a.id, a.name from dm.dm_application a where a.isRelease='Y' and a.status='N' and a.domainid=? and "
     + "exists (select x.id from dm.dm_application x where x.id=a.parentid and x.domainid <> a.domainid) order by name";
   sql_access = "select count(*) from dm.dm_applicationaccess where appid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_applicationaccess where appid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case APPVERSION:
   sql = "select a.id, a.name from dm.dm_application a where a.isRelease<> 'Y' and a.status='N' and a.domainid=? and "
    + "exists (select x.id from dm.dm_application x where x.id=a.parentid and x.domainid <> a.domainid and x.status='N') order by name";
   sql_access = "select count(*) from dm.dm_applicationaccess where appid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_applicationaccess where appid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case COMPONENT:
   if (catid != -1) {
	   // Category set.
	   if (domainID == 0) {
		   // Any Domain up from our home domain
		   sql = "select a.id,a.name,b.usrgrpid,b.viewaccess   "
			 + "from     dm.dm_component a   "
			 + "left outer join  dm.dm_componentaccess b on a.id=b.compid "
			 + "inner join dm.dm_component_categories c on a.id=c.id "
			 + " where    a.status='N' and c.categoryid=? and a.domainid in ("+m_parentdomains+") and predecessorid is null order by a.name ";
		   stmt = getDBConnection().prepareStatement(sql);
		   stmt.setInt(1,catid);
	   } else {
		   // Specific Domain
		   sql = "select a.id,a.name,b.usrgrpid,b.viewaccess   "
		     + "from     dm.dm_component a   "
		     + "left outer join  dm.dm_componentaccess b on a.id=b.compid "
		     + "inner join dm.dm_component_categories c on a.id=c.id "
		     + " where    a.status='N' and a.domainid=? and c.categoryid=? and predecessorid is null order by a.name ";
		   stmt = getDBConnection().prepareStatement(sql);
		   stmt.setInt(1,domainID);
		   stmt.setInt(2,catid);
	   }
   } else {
	   // Any Category.
	   if (domainID == 0) {
		   // Any Domain
		   sql = "select a.id,a.name,b.usrgrpid,b.viewaccess   "
				     + "from     dm.dm_component a   "
				     + "left outer join  dm.dm_componentaccess b on a.id=b.compid "
				     + " where    a.status='N' and a.domainid in ("+m_parentdomains+") and predecessorid is null order by a.name ";
		   stmt = getDBConnection().prepareStatement(sql);
	   } else {
		   // Specific Domain
		   sql = "select a.id,a.name,b.usrgrpid,b.viewaccess   "
		     + "from     dm.dm_component a   "
		     + "left outer join  dm.dm_componentaccess b on a.id=b.compid "
		     + " where    a.status='N' and a.domainid=? and predecessorid is null order by a.name ";
		   stmt = getDBConnection().prepareStatement(sql);
		   stmt.setInt(1,domainID);
	   }
   } 
   newQuery=true;
   break;
  case COMPVERSION:
   sql =  "select a.id,a.name,b.usrgrpid,b.viewaccess   "
     + "from     dm.dm_component a   "
     + "left outer join  dm.dm_componentaccess b on a.id=b.compid "
     + " where    a.status='N' and a.domainid=? and predecessorid >= 0 order by a.name ";
   // sql = "select id,name from dm.dm_component where status='N' and domainid=? and predecessorid >= 0 order by name";
   sql_access = "select count(*) from dm.dm_componentaccess where compid = ? and usrgrpid = ? and viewaccess = 'N'";   
   sql_hasaccess = "select count(*) from dm.dm_componentaccess where compid = ? and usrgrpid = ? and viewaccess = 'Y'";    
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   newQuery=true;
   break; 
  case CREDENTIALS:
   sql =  "select id,name from dm.dm_credentials where status='N' and domainid=? order by name";
   sql_access = "select count(*) from dm.dm_credentialsaccess where credid = ? and usrgrpid = ? and viewaccess = 'N'";
    sql_hasaccess = "select count(*) from dm.dm_credentialsaccess where credid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case ACTION: // Graphical actions
   sql =  "select a.id,a.name from dm.dm_action a,dm.dm_action_categories b,  dm.dm_category c where status='N' and a.id = b.id and b.categoryid = c.id and a.domainid=? and c.id=? and a.kind="+ActionKind.GRAPHICAL.value()+" order by a.name";
   sql_access = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   stmt.setInt(2, catid);   
   break;
  case SERVERCOMPTYPE: 
   sql =  "select id,name from dm.dm_type where status='N' and domainid=? order by name";
   sql_access = "select count(*) from dm.dm_typeaccess where comptypeid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_typeaccess where comptypeid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break; 
  case PROCEDURE:
  case FUNCTION:
   // sql =  "select id,name,kind from dm.dm_action where status='N' and domainid=? and NOT kind="+ActionKind.GRAPHICAL.value()+" and function=? order by name";
   // sql_access = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'N'";
   // sql_hasaccess = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'Y'";
   System.out.println("Opening category "+catid+" domain="+domainID);
   sql =  	"select a.id,a.name,b.usrgrpid,b.viewaccess,a.kind   "
   +   		"from     			dm.dm_action a   "
   +   		"left outer join 	dm.dm_actionaccess b on a.id=b.actionid "
   +		"inner join			dm.dm_fragments f on a.id in (f.actionid,f.functionid) "		
   + 		"inner join			dm.dm_fragment_categories c on f.id=c.id "
   +   		"where				a.status='N' "
   +  		"and    			a.domainid=? and c.categoryid=? "
   +  		"and    			a.kind != "+ActionKind.GRAPHICAL.value()+" "
   +  		"and     			a.function=? order by a.name ";
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   stmt.setInt(2, catid);
   stmt.setString(3, (ot == ObjectType.FUNCTION) ? "Y" : "N");
   newQuery=true;
   break;
  case COMP_CATEGORY:
	  System.out.println("Retrieving COMP_CATEGORY for domain "+domainID);
   sql =  "select distinct c.id,c.name from dm.dm_component a, dm.dm_component_categories b,  dm.dm_category c where a.status='N' and a.domainid=? and predecessorid is null and a.id = b.id and b.categoryid = c.id order by c.name";
   sql_access = "select count(*) from dm.dm_componentaccess where compid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_componentaccess where compid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break; 
  case ACTION_CATEGORY:
   sql =  "select distinct c.id,c.name from dm.dm_action a, dm.dm_action_categories b,  dm.dm_category c where a.status='N' and a.domainid=? and kind="+ActionKind.GRAPHICAL.value()+" and a.id = b.id and b.categoryid = c.id order by c.name";
   sql_access = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'Y'";
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case PROCFUNC_CATEGORY:
	  // was action_categories
   // sql =  "select distinct c.id,c.name from dm.dm_action a, dm.dm_fragment_categories b,  dm.dm_category c where a.status='N' and a.domainid=? and NOT kind="+ActionKind.GRAPHICAL.value()+" and a.id = b.id and b.categoryid = c.id order by c.name";
   sql = "select distinct c.id,c.name from dm.dm_fragments f, dm.dm_action a, dm.dm_fragment_categories b,  dm.dm_category c where a.status='N' and a.domainid=? and a.kind != "+ActionKind.GRAPHICAL.value()+" and a.id in (f.actionid,f.functionid) and f.id = b.id and b.categoryid = c.id order by c.name";
   sql_access = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'Y'";
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case FRAGMENT:
   sql =  "select distinct a.id,a.name, a.actionid,a.functionid from dm.dm_fragments a, dm.dm_fragment_categories b where a.id = b.id and b.categoryid = ? order by name";
   // sql_access = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'N'";
   // sql_hasaccess = "select count(*) from dm.dm_actionaccess where actionid = ? and usrgrpid = ? and viewaccess = 'Y'";
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, catid);
   break;   
  case ENVIRONMENT:
   sql =  "select a.id,a.name,b.usrgrpid,b.viewaccess   "
     + "from     dm.dm_environment a   "
     + "left outer join  dm.dm_environmentaccess b on a.id=b.envid "
     + " where    a.status='N' and a.domainid=? order by a.name ";
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   newQuery=true;
   break;
  case USERGROUP:
   sql = "select a.id,a.name from dm.dm_usergroup a where a.status='N' and a.domainid=? order by a.name";
   sql_access = "select count(*) from dm.dm_usergroupaccess where usergroupid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_usergroupaccess where usergroupid = ? and usrgrpid = ? and viewaccess = 'Y'";
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case USER:
   sql = "select a.id,a.name from dm.dm_user a where a.status='N' and a.domainid=? order by a.name";
   sql_access = "select count(*) from dm.dm_useraccess where userid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_useraccess where userid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case DATASOURCE:
   sql = "select a.id,a.name from dm.dm_datasource a where a.status='N' and a.domainid=? order by a.name";
   sql_access = "select count(*) from dm.dm_datasourceaccess where datasourceid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_datasourceaccess where datasourceid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case DOMAIN:
   if (domainID > 0) {
    Domain d = getDomain(domainID);
    String ordClause = d!=null?(d.getLifecycle())?"position":"name":"name";
    sql =  "select    a.id,a.name,b.usrgrpid,b.viewaccess  "
      + "from    dm.dm_domain a       "
      + "left outer join dm.dm_domainaccess b on a.id=b.domainid "
      + "where    a.status='N' and a.domainid=? order by a."+ordClause;
    stmt = getDBConnection().prepareStatement(sql);
    stmt.setInt(1, domainID);
   } else if (domainID == 0) {
    sql =   "select    a.id,a.name,b.usrgrpid,b.viewaccess  "
      + "from    dm.dm_domain a       "
      + "left outer join dm.dm_domainaccess b on a.id=b.domainid "
      + "where    a.status='N' and a.id=? order by a.name";
    stmt = getDBConnection().prepareStatement(sql);
    stmt.setInt(1,m_userDomain);
   } else {
    // domainID is < 0 - must be the "inherited" domain. No subdomains here
    stmt = null;
   }
   newQuery=true;
   break;
  case NOTIFY:
   sql = "select a.id,a.name from dm.dm_notify a where a.status='N' and a.domainid=? order by a.name";
   sql_access = "select count(*) from dm.dm_notifyaccess where notifyid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_notifyaccess where notifyid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case REPOSITORY:
   sql = "select a.id,a.name from dm.dm_repository a where a.status='N' and a.domainid=? order by a.name";
   sql_access = "select count(*) from dm.dm_repositoryaccess where repositoryid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_repositoryaccess where repositoryid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case SERVER:
   sql = "select a.id,a.name from dm.dm_server a where a.status='N' and a.domainid=? order by a.name";
   sql_access = "select count(*) from dm.dm_serveraccess where serverid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_serveraccess where serverid = ? and usrgrpid = ? and viewaccess = 'Y'";
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  case BUILDER:
   sql =  "select id,name from dm.dm_buildengine where status='N' and domainid=? order by name";
   sql_access = "select count(*) from dm.dm_buildengineaccess where builderid = ? and usrgrpid = ? and viewaccess = 'N'";
   sql_hasaccess = "select count(*) from dm.dm_buildengineaccess where builderid = ? and usrgrpid = ? and viewaccess = 'Y'";   
   stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainID);
   break;
  default:
   throw new RuntimeException("Unhandled object type " + ot + " when getting tree objects");   
  }
  
  if (newQuery)
  {
   ArrayList<Integer> dups = new ArrayList<Integer>();
   long startTime = System.nanoTime();
   
   // System.out.println("***** START ********");
    if (stmt == null) return ret; // nothing to retrieve.
   ResultSet rs = stmt.executeQuery();
   vagc.clear();
   for (Integer gid : vag.keySet()) {
       vagc.put(gid,vag.get(gid));
   }
   int lastid=-1;
   int objid=0;
   int lastkind=0;
   String objname=null;
   String lastobjname=null;
   int kind=0;
   while (rs.next()) {
    objid = rs.getInt(1);
    objname = rs.getString(2);
    int objGroupID=rs.getInt(3);
    String yorn=rs.getString(4); // Y or N (allow or deny view access)
    boolean inherit=rs.wasNull(); // if not set then inherit from parent domain
    kind = (ot == ObjectType.PROCEDURE || ot == ObjectType.FUNCTION)?rs.getInt(5):0;
    System.out.println("xx) objid="+objid+" objname=["+objname+"] kind="+kind);
    // System.out.println("Row: id:"+objid+" name:"+objname+" groupid:"+objGroupID+" yorn:"+yorn+" inherit:"+inherit);
    boolean Allow=false;
    
    if (objid != lastid && lastid != -1) {
     // End of entries for this object
     // We need at least one group of which we're a member with view access to allow us to view the object
     // System.out.println("Testing access for " + ot + " "+lastid);
     if (m_OverrideAccessControl) {
//      System.out.println("1) User is SUPERUSER");
      // Ignore the access control
      TreeObject treeObject = new TreeObject(lastid,lastobjname);
      treeObject.SetObjectType(ot);
      if (lastkind>0) treeObject.SetObjectKind(lastkind);
      if (!dups.contains(lastid)) {
       dups.add(new Integer(lastid));
       System.out.println("1) Adding new TreeObject("+lastid+",\""+lastobjname+"\") lastkind="+lastkind);
       ret.add(treeObject);
      }  
     } else { 
      for (Integer u: gm) {
       if (vagc.containsKey(u) && vagc.get(u).equalsIgnoreCase("Y")) {
        // found a "Y" - we're allowed to view, add it and move on.
        // System.out.println("found u="+u+" in vagc, adding object "+lastid+"objname="+lastobjname);
        TreeObject treeObject = new TreeObject(lastid,lastobjname);
        treeObject.SetObjectType(ot);
        if (lastkind>0) treeObject.SetObjectKind(lastkind);
        if (!dups.contains(lastid)) {
         dups.add(new Integer(lastid));
         System.out.println("2) Adding new TreeObject("+lastid+",\""+lastobjname+"\") lastkind="+lastkind);
         ret.add(treeObject);
        }  
        break;
       }
      }
     }
     vagc.clear();
     for (Integer gid : vag.keySet()) {
         vagc.put(gid,vag.get(gid));
     }
    }
    
    if (!inherit) {
     Allow=yorn.equalsIgnoreCase("y");
  //   System.out.println("Adding group "+objGroupID+" to list (Allow="+Allow+") yorn="+yorn);
     vagc.put(objGroupID,(Allow?"Y":"N"));
    }
    
    lastid=objid;
    lastobjname=objname;
    lastkind=kind;
   }
   // End of list
   System.out.println("End of list, objid="+objid+" lastid="+lastid+" objname="+objname+" lastobjname="+lastobjname+" lastkind="+lastkind);
   if (lastid != -1) {
    // System.out.println("Testing access for " + ot + " "+lastid);
    if (m_OverrideAccessControl) {
     // System.out.println("2) User is SUPERUSER");
     // Ignore the access control
     TreeObject treeObject = new TreeObject(lastid,lastobjname);
     treeObject.SetObjectType(ot);
     if (lastkind>0) treeObject.SetObjectKind(lastkind);
     if (!dups.contains(lastid)) {
      dups.add(new Integer(lastid));
      System.out.println("Adding new TreeObject("+lastid+",\""+lastobjname+"\") lastkind="+lastkind);
      ret.add(treeObject);
     }  
    } else { 
     for (Integer u: gm) {
      // System.out.println("Checking group "+u);
      if (vagc.containsKey(u) && vagc.get(u).equalsIgnoreCase("Y")) {
       // found a "Y" - we're allowed to view, add it and move on.
       TreeObject treeObject = new TreeObject(lastid,lastobjname);
       treeObject.SetObjectType(ot);
       if (lastkind>0) treeObject.SetObjectKind(lastkind);
       if (!dups.contains(lastid)) {
        dups.add(new Integer(lastid));
        System.out.println("Adding new TreeObject("+lastid+",\""+lastobjname+"\") lastkind="+lastkind);
        ret.add(treeObject);
       }  
       break;
      }
     }
    }
    long endTime = System.nanoTime();
    System.out.println("**** Time Taken "+(endTime-startTime)+" nanosecs");
   }
   rs.close();
   // System.out.println("******* END *******");
  }
  else
  {
   // old code for other objects (temp)
//   System.out.println("ot.name="+ot.name());
   ArrayList<Integer> dups = new ArrayList<Integer>();

   ResultSet rs = stmt.executeQuery();
   while (rs.next()) {
    int hasDeny = 0;
    int hasAllow = 0;
    
    if (sql_access.length() > 0) {
     PreparedStatement stmt_access = getDBConnection().prepareStatement(sql_access);
     PreparedStatement stmt_hasaccess = getDBConnection().prepareStatement(sql_hasaccess);
     for (Integer u: gm) {
      stmt_access.setInt(1, rs.getInt(1));
      stmt_access.setInt(2, u);
      ResultSet rs2 = stmt_access.executeQuery();
      while (rs2.next()) {
       hasDeny  += rs2.getInt(1);
      }
      rs2.close();

      stmt_hasaccess.setInt(1, rs.getInt(1));
      stmt_hasaccess.setInt(2, u);
      ResultSet rs3 = stmt_hasaccess.executeQuery();
      while (rs3.next()) {
       hasAllow  += rs3.getInt(1);
      }
      rs3.close();
     }
     stmt_access.close();
     stmt_hasaccess.close();
    }
    if (hasAllow >= 1 || getAclOverride()) {
     // found a group with explicit view permission - add it and move on.
     TreeObject treeObject = new TreeObject(rs.getInt(1),rs.getString(2));
     treeObject.SetObjectType(ot);
     if (ot == ObjectType.PROCEDURE || ot == ObjectType.FUNCTION) {
      treeObject.SetObjectKind(rs.getInt(3));
     } else if (ot == ObjectType.FRAGMENT) {
      int actionid = rs.getInt(3);
      int functionid = rs.getInt(4);
      ObjectType t = ot;
      if (actionid>0) t = ObjectType.FRAGMENTPROC;
      else
      if (functionid>0) t = ObjectType.FRAGMENTFUNC;
      System.out.println("a) ["+rs.getString(2)+"] objecttype="+t);
      treeObject.SetObjectType(t);
     }
     ret.add(treeObject);
    } else if (hasDeny == 0) {
     // No explicit view access and no explicit deny - check domain access
     for (Integer u: gm) {
      if (vag.containsKey(u) && vag.get(u).equalsIgnoreCase("Y")) {
       // found a "Y" - we're allowed to view, add it and move on.
       TreeObject treeObject = new TreeObject(rs.getInt(1),rs.getString(2));
       if (ot == ObjectType.FRAGMENT) {
        int actionid = rs.getInt(3);
        int functionid = rs.getInt(4);
        ObjectType t = ot;
        if (actionid>0) t = ObjectType.FRAGMENTPROC;
        else
        if (functionid>0) t = ObjectType.FRAGMENTFUNC;
        System.out.println("b) objecttype="+t);
        treeObject.SetObjectType(t);
       } else {
        System.out.println("c) objecttype="+ot);
        treeObject.SetObjectType(ot);
       }
       if (!dups.contains(rs.getInt(1))) {
        dups.add(new Integer(rs.getInt(1)));
        ret.add(treeObject);
       }  
       break;
      }
     }
    }
   }
   rs.close();
  }  
  stmt.close();
  return ret;

 } catch (SQLException e) {
  e.printStackTrace();
  rollback();
 }
 throw new RuntimeException("Unable to retrieve object from database");
}


/*
 private void AddCategories2ProcFunc(TreeObject treeObj, int id, String name)
 {
   Category c = new Category(id, name);

   if (treeObj.GetCategories() == null) {
     treeObj.SetCategories(new ArrayList<Category>());
   }
   ArrayList<Category> res = treeObj.GetCategories();
   res.add(c);
 }
*/
	public List<TreeObject> getInheritedTreeObjects(ObjectType ot, int domainID)
	{
		// moves up from the specified domain and finds objects of the specified type in each
		// parent/grandparent etc
		List<TreeObject> ret = new ArrayList<TreeObject>();
		Domain dom = getDomain(domainID);
		dom = dom.getDomain();	// parent domain
		while (dom != null) {
			ret.addAll(getTreeObjects(ot,dom.getId(),-1));
			dom = dom.getDomain();	// parent domain
		}
		return ret;
	}

	public String getParentDomainsForObject(String otid)
	{
		System.out.println("getParentDomainsForObject("+otid+")");
		String res="";
		String sep="do";
		DMObject go = null;
		if (otid.substring(0,2).equalsIgnoreCase("co") || otid.substring(0,2).equalsIgnoreCase("cv")) {
			// Component / Component Version
			Component comp = getComponent(Integer.parseInt(otid.substring(2)),true);
			Category cat = comp.getCategory();
			if (comp.getParentId()>0) {
				// This is a component version - add the parent version
				Component parcomp = getComponent(comp.getParentId(),false);
				res+="co"+parcomp.getId();
				if (cat != null) {
					res+=" cc"+cat.getId()+"-"+comp.getDomainId();
				}
				sep=" do";
				go=parcomp;
			} else {
				if (cat != null) {
					res+="cc"+cat.getId()+"-"+comp.getDomainId();
					sep=" do";
				}
				go = comp;
			}
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("ap") || 
			otid.substring(0,2).equalsIgnoreCase("av") ||
			otid.substring(0,2).equalsIgnoreCase("rl")) {
			// Application / Application Version / Release
			Application app = getApplication(Integer.parseInt(otid.substring(2)),false);
			boolean isRelease=false;
			if (app.getIsRelease().equalsIgnoreCase("y")) {
				// This is a release
				isRelease=true;
			}
			if (app.getParentId()>0) {
				// This is a application version - if the parent is not in a lifecycle domain then the
				// version will be under the parent in the tree view
				Application parapp = getApplication(app.getParentId(),false);
				if (parapp.getDomain().getLifecycle() == false) {
					res+=isRelease?"rl":"ap"+parapp.getId();
					sep=" do";
					go=parapp;
				} else {
					go=app;
				}
			} else {
				go = app;
			}
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("ac")) {
			// Action
			Action action = getAction(Integer.parseInt(otid.substring(2)),true);
			Category cat = action.getCategory();
			if (action.getParentId()>0) {
				// This is an archived action - will be a child of the parent
				res+="ac"+action.getParentId();
				if (cat != null) {
					res+=" cy"+cat.getId()+"-"+action.getDomainId();
				}
				sep=" do";
				Action paraction = getAction(action.getParentId(),false);
				go = paraction;
			} else {
				if (cat != null) {
					res+="cy"+cat.getId()+"-"+action.getDomainId();
					sep=" do";
				}
				go = action;
			}
			System.out.println("res initially "+res);
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("te")) {
			// Notifiers
			NotifyTemplate template = getTemplate(Integer.parseInt(otid.substring(2)));
			int notifierid = template.getNotifierId();
			Notify notifier = this.getNotify(notifierid,false);
			if (notifier != null) {
				res+="no"+notifier.getId();
				sep=" do";
				go = notifier;
			} else {
				go = template;
			}
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("se")) {
			// Server
			Server server = getServer(Integer.parseInt(otid.substring(2)),false);
			go = server;	
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("en")) {
			// Environment
			Environment env = getEnvironment(Integer.parseInt(otid.substring(2)),false);
			go = env;	
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("pr") || otid.substring(0,2).equalsIgnoreCase("fn")) {
			// Procedure/Function
			String kind = otid.substring(2).replaceAll("^.*-","");
			String id = otid.substring(2).replaceAll("-.*$","");
			Action action = getAction(Integer.parseInt(id),true);
			System.out.println("action is "+action.getName()+" id "+action.getId());
			Category cat = action.getCategory();
			System.out.println("category is "+cat.getName()+" id "+cat.getId());
			if (action.getParentId()>0) {
				// This is an archived action - will be a child of the parent
				res+=otid.substring(0,2)+action.getParentId()+"-"+kind;
				if (cat != null) {
					res+=" cp"+cat.getId()+"-"+action.getDomainId();
				}
				sep=" do";
				Action paraction = getAction(action.getParentId(),false);
				go = paraction;
			} else {
				if (cat != null) {
					res+="cp"+cat.getId()+"-"+action.getDomainId();
					sep=" do";
				}
				go = action;
			}
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("do")) {
			// Domain
			String id = otid.substring(2);
			Domain domain = getDomain(Integer.parseInt(id));
			go = domain;
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("ds")) {
			// Datasource
			String id = otid.substring(2);
			Datasource ds = this.getDatasource(Integer.parseInt(id),true);
			res+="ds"+ds.getId();
			go = ds;
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("be")) {
			// Build Engine
			Builder builder = getBuilder(Integer.parseInt(otid.substring(2)));
			go = builder;	
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("bj")) {
			// Build Job
			BuildJob buildjob = getBuildJob(Integer.parseInt(otid.substring(2)));
			int builderid = buildjob.getBuilderId();
			Builder builder = getBuilder(builderid);
			if (builder != null) {
				res+="be"+builder.getId();
				sep=" do";
				go = builder;
			} else {
				go = buildjob;
			}
						
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("us")) {
			// User
			User user = getUser(Integer.parseInt(otid.substring(2)));
			go = user;
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("gr")) {
			// User
			UserGroup group = getGroup(Integer.parseInt(otid.substring(2)));
			go = group;
		}
		else
		if (otid.substring(0,2).equalsIgnoreCase("re")) {
			// Repository
			Repository repo = getRepository(Integer.parseInt(otid.substring(2)),false);
			go = repo; 
		} 
		Domain dom = go.getDomain();
		System.out.println("dom="+dom.getId());
		while (dom != null) {
			res=res+sep+dom.getId();
			sep=" do";
			if (dom.getId() == m_userDomain) break;	// reached top of hierarchy
			dom = dom.getDomain();
			System.out.println("dom="+dom.getId());
		}
		System.out.println("returning "+res);
		return res;
	}

	
	public List<DMObject> getDMObjects(ObjectType ot,boolean subdomains)
	{
		// Returns a list of DMObjects that are in the user's domain (or subdomains if flag is set)
		List <DMObject> ret = new ArrayList<DMObject>();

		String domains = subdomains?m_domainlist:Integer.toString(m_userDomain);
		try
		{
			System.out.println("ObjectType=["+ot+"]");
			/*
			PreparedStatement stmt;
			switch(ot) {
			case APPLICATION:
				sql = "select a.id from dm.dm_application a where a.status='N' and a.domainid in ("+domains+") and "
					+ "(a.predecessorid is null or exists "
					+ "(select x.id from dm.dm_application x where x.id=a.parentid and x.domainid <> a.domainid)) order by name";
				stmt = m_conn.prepareStatement(sql);
				break;
			case COMPONENT:
				sql = "select id from dm.dm_component where status='N' domainid in ("+domains+") order by name";
				stmt = m_conn.prepareStatement(sql);
			default:
				sql = "select id from dm.dm_"+ot.toString()+" where status='N' and domainid in ("+domains+") order by name";
				stmt = m_conn.prepareStatement(sql);
			}
			*/
			String sql = "SELECT id FROM dm.dm_"+ot.toString()+" WHERE status='N' and domainid in ("+domains+") order by name";
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				switch(ot) {
				case APPLICATION:
					Application app = getApplication(rs.getInt(1),true);
					ret.add(app);
					break;
				case COMPONENT:
					Component comp = getComponent(rs.getInt(1),true);
					ret.add(comp);
					break;
				case ENVIRONMENT:
					Environment env = getEnvironment(rs.getInt(1),true);
					ret.add(env);
					break;
				case SERVER:
					Server server = getServer(rs.getInt(1),true);
					ret.add(server);
					break;
				default:
					break;
				}
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve object " +ot.toString()+" from database");
	}
	
	public List<Server> getServersInEnvironment(int EnvironmentID)
	{
		List <Server> ret = new ArrayList<Server>();
		String sql="select a.id,a.name,a.summary,a.hostname,b.xpos,b.ypos from dm.dm_server a,dm.dm_serversinenv b where b.envid=? and a.id=b.serverid and a.status = 'N' order by name";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, EnvironmentID);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				Server dmobject = new Server(this,rs.getInt(1),rs.getString(2));
				dmobject.setSummary(rs.getString(3));
				dmobject.setHostName(rs.getString(4));
				dmobject.setXpos(rs.getInt(5));
				dmobject.setYpos(rs.getInt(6));
				ret.add(dmobject);
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve object servers for environment from database");
	}
	
	List<ComponentLink> getComponentLinks(int appid)
	{
		List <ComponentLink> ret = new ArrayList<ComponentLink>();
		String sql = "SELECT appid,objfrom,objto FROM dm.dm_applicationcomponentflows WHERE appid=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,appid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				ComponentLink cl = new ComponentLink();
				
				cl.setAppId(rs.getInt(1));
				cl.setObjFrom(rs.getInt(2));
				cl.setObjTo(rs.getInt(3));
				ret.add(cl);
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve object component links for application from database");
	}
	
	public List<Component> getComponents(ObjectType objtype, int id, boolean isRelease)
	{
		List <Component> ret = new ArrayList<Component>();
		String sql="";
		int cols=0;
		switch(objtype) {
		case SERVER:
			sql="SELECT a.id, a.name, a.domainid, a.summary, a.parentid, a.rollup, a.rollback, a.filteritems, a.lastbuildnumber, a.buildjobid "
				+ "FROM dm.dm_component a,dm.dm_compsallowedonserv b "
				+ "where a.status = 'N' and a.id = b.compid and b.serverid=?";
			cols=10;
			break;
		case ENVIRONMENT:
			sql="";
			break;
		case DOMAIN:
			sql="select a.id, a.name, a.domainid, a.summary, a.parentid, a.rollup, a.rollback, a.filteritems, a.lastbuildnumber, a.buildjobid "
				+ "FROM dm.dm_component a where  a.status = 'N' and a.domainid in ("+m_domainlist+")";
			cols=10;
			break;
		case APPLICATION:
		 if (isRelease)
			 sql="select a.id, a.name, a.domainid, a.summary, a.parentid, 0, 0, 'N',0,0,"
			 	+ "b.xpos, b.ypos FROM dm.dm_application a, dm.dm_applicationcomponent b "
			 	+ "where  a.status = 'N' and  b.appid=? and a.id = b.childappid";
		 else
			 sql="select a.id, a.name, a.domainid, a.summary, a.parentid, a.rollup, a.rollback, a.filteritems, a.lastbuildnumber, a.buildjobid, "
				+ "b.xpos, b.ypos FROM dm.dm_component a, dm.dm_applicationcomponent b "
				+ "where  a.status = 'N' and b.appid=? and a.id = b.compid";
			cols=12;
			break;
		default:
			break;
		}

		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, id);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				Component comp = new Component(this,rs.getInt(1),rs.getString(2));
				comp.setDomainId(rs.getInt(3));
				comp.setSummary(rs.getString(4));
				comp.setParentId(getInteger(rs,5,0));
				comp.setRollup(ComponentFilter.fromInt(getInteger(rs, 6, 0)));
				comp.setRollback(ComponentFilter.fromInt(getInteger(rs, 7, 0)));
				comp.setFilterItems(getBoolean(rs, 8));
				comp.setLastBuildNumber(rs.getInt(9));
				int buildjobid = getInteger(rs,10,0);
				if (buildjobid > 0) {
					BuildJob buildjob = getBuildJob(buildjobid);
					comp.setBuildJob(buildjob);
				}
				if (cols>10) {
					comp.setXpos(rs.getInt(11));
					comp.setYpos(rs.getInt(12));
					
				}
				ret.add(comp);
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve object components for server from database");
	}
	
	public TableDataSet getComponentsOnServer(Server server)
	{
		String sql = "	SELECT a1.id, a1.name, a2.id, a2.name, a2.predecessorid, a2.parentid,  "
				+ "  ai.deploymentid, d.exitcode, d.finished,   "
				+ "  a1.modifierid, u.name, u.realname, "
				+ "  ai.modified,ai.buildnumber	"
				+ "  FROM  dm.dm_compsonserv ai	"
				+ "  LEFT OUTER JOIN dm.dm_deployment d ON d.deploymentid = ai.deploymentid	"
				+ "  LEFT JOIN dm.dm_component a2 ON a2.id = ai.compid	"
				+ "  LEFT JOIN dm.dm_component a1 ON a1.id in (a2.id,a2.parentid)	"
				+ "  LEFT OUTER JOIN dm.dm_user u ON u.id = ai.modifierid	"
				+ "  WHERE ai.serverid = ? and a1.parentid is null	"
				+ "	 AND a1.status<>'D'	"
				+ "	 AND a2.status<>'D' "
				+ "  ORDER BY 2	";
		
		
		System.out.println(sql);
		System.out.println("serverid="+server.getId());
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, server.getId());
			ResultSet rs = stmt.executeQuery();
			TableDataSet ret = new TableDataSet(5);
			int row = 0;
			while(rs.next()) {
				ret.put(row, 0, new JSONBoolean(false));
				ret.put(row, 1, new Component(this, rs.getInt(1), rs.getString(2)).getLinkJSON());
				int avid = getInteger(rs, 3, 0);
				if(avid != 0) {
					Component comp = new Component(this, avid, rs.getString(4));
					comp.setPredecessorId(getInteger(rs, 5, 0));
					comp.setParentId(getInteger(rs, 6, 0));
					ret.put(row, 2, comp.getLinkJSON());
					int deploymentid = getInteger(rs, 7, 0);
					if(deploymentid != 0) {
						Deployment d = new Deployment(this, deploymentid, rs.getInt(8));
						d.setFinished(rs.getInt(9));
						ret.put(row, 3, d.getLinkJSON());
					} else {
						ret.put(row, 3, new CreatedModifiedField(
								formatDateToUserLocale(rs.getInt(13)),
								new User(this, rs.getInt(10), rs.getString(11), rs.getString(12))));						
					}
					int buildno = getInteger(rs,14,0);
					ret.put(row, 4, buildno);
				}
				row++;
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve component version info for server '" + server.getName() + "' from database");
	}
	
 public ArrayList<Component> getComponentsOnServerList(Server server)
 {
  String sql = " SELECT a1.id, a1.name, a2.id, a2.name, a2.predecessorid, a2.parentid,  "
    + "  ai.deploymentid, d.exitcode, d.finished,   "
    + "  a1.modifierid, u.name, u.realname, "
    + "  a1.modified,ai.buildnumber "
    + "  FROM  dm.dm_compsonserv ai "
    + "  LEFT OUTER JOIN dm.dm_deployment d ON d.deploymentid = ai.deploymentid "
    + "  LEFT JOIN dm.dm_component a2 ON a2.id = ai.compid "
    + "  LEFT JOIN dm.dm_component a1 ON a1.id in (a2.id,a2.parentid) "
    + "  LEFT OUTER JOIN dm.dm_user u ON u.id = a2.modifierid "
    + "  WHERE ai.serverid = ? and a1.parentid is null "
    + "  AND a1.status<>'D' "
    + "  AND a2.status<>'D' "
    + "  ORDER BY 2 ";
  
  ArrayList<Component> ret = new ArrayList<Component>();
  
  System.out.println(sql);
  System.out.println("serverid="+server.getId());
  try
  {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, server.getId());
   ResultSet rs = stmt.executeQuery();
   while(rs.next()) {
//    ret.add(new Component(this, rs.getInt(1), rs.getString(2)));
    int avid = getInteger(rs, 3, 0);
    if(avid != 0) {
     Component comp = new Component(this, avid, rs.getString(4));
     comp.setPredecessorId(getInteger(rs, 5, 0));
     comp.setParentId(getInteger(rs, 6, 0));
     ret.add(comp);
    }
   }
   rs.close();
   stmt.close();
   return ret;
  }
  catch(SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  return ret;
 }
 
	public List<Application> getAppsForComponent(Component comp)
	{
		List<Application> res = new ArrayList<Application>();
		String sql="SELECT a.appid FROM dm.dm_applicationcomponent a,dm.dm_application b WHERE a.compid=? AND a.appid=b.id and b.status='N'";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,comp.getId());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Application app = getApplication(rs.getInt(1),true);
				res.add(app);
			}
			rs.close();
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve associated applications for component '" + comp.getName() + "' from database");
	}
	
	public TableDataSet getBuildsForComponent(Component comp)
	{
		System.out.println("Getting builds for component "+comp.getId());
		String sql;
		if (comp.getParentId()==0) {
			// This is a base version - include all versions of component
			sql="SELECT a.buildjobid,b.name,a.compid,c.name,c.parentid,a.buildnumber,a.timestamp,a.success " +
				"FROM dm.dm_buildhistory a,dm_buildjob b,dm_component c WHERE a.compid IN " +
				"(SELECT x.id FROM dm.dm_component x WHERE x.parentid = ? " +
				"UNION SELECT y.id FROM dm.dm_component y WHERE y.id = ?) " +
				"AND a.buildjobid=b.id AND c.id = a.compid AND c.status='N' ORDER BY buildnumber DESC";
		} else {
			// Specific Component Version - just include this version
			sql="SELECT a.buildjobid,b.name,a.compid,c.name,c.parentid,a.buildnumber,a.timestamp,a.success " +
				"FROM dm.dm_buildhistory a,dm_buildjob b, dm_component c WHERE a.compid=? " +
				"AND a.buildjobid=b.id AND c.id = a.compid AND c.status='N' ORDER BY buildnumber DESC";
		}
		
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,comp.getId());
			if (comp.getParentId()==0) stmt.setInt(2,comp.getId());
			ResultSet rs = stmt.executeQuery();
			TableDataSet ret = new TableDataSet(5);
			int row = 0;
			while(rs.next()) {
				System.out.println("*** buildno="+rs.getInt(5)+" success="+rs.getString(7));
				ret.put(row, 0, new BuildJob(this, rs.getInt(1), rs.getString(2)).getLinkJSON());
				Component c = new Component(this, rs.getInt(3), rs.getString(4));
				c.setParentId(rs.getInt(5));
				ret.put(row, 1, c.getLinkJSON());
				ret.put(row, 2, rs.getInt(6));
				ret.put(row, 3, formatDateToUserLocale(rs.getInt(7)));
				ret.put(row, 4, rs.getString(8));
				row++;
			}
			rs.close();
			stmt.close();
			return ret;
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve component build history for component '" + comp.getName() + "' from database");
	}
	
	public TableDataSet getComponentLocations(Component comp,String t, boolean isRelease)
	{
		String sql="";
		int parentid = comp.getParentId();
		// boolean cv = (parentid>0);
		System.out.println("In getComponentLocations, t="+t);
		boolean bServers=false;
		if (t.equalsIgnoreCase("S")) {
			// Finding components on servers
			bServers=true;
			//String col=cv?"parentid":"id";
			System.out.println("Doing components for servers id="+comp.getId()+" parentid="+parentid);
			sql = "SELECT	e.id,e.name,s.id,s.name,d.id,d.name,d.parentid,	"
				+	"		cos.deploymentid, p.exitcode, cos.buildnumber, p.finished,	"
				+	"			cos.modifierid, u.name, u.realname, d.modified	"	
				+	"		FROM	dm.dm_component c,		"
				+	"			dm.dm_server s	"
				+	"		LEFT OUTER JOIN dm.dm_compsonserv cos ON cos.serverid = s.id	"
				+	"		LEFT OUTER JOIN dm.dm_component d ON d.id = cos.compid AND (d.id in (select q.id from dm.dm_component q where q.parentid=?) or d.id=? or d.id in (select distinct q.parentid from dm.dm_component q where q.id=?)) "
				+	"		LEFT OUTER JOIN dm.dm_serversinenv sie ON sie.serverid = s.id	"
				+	"		LEFT OUTER JOIN dm.dm_environment e ON e.id = sie.envid	"
				+	"		LEFT OUTER JOIN dm.dm_deployment p ON p.deploymentid = cos.deploymentid	"
				+	"		LEFT OUTER JOIN dm.dm_user u ON u.id = cos.modifierid	"
				+	"		WHERE	? in (c.parentid,c.id)	"
				+	"		AND	cos.compid=c.id	"
				+	"		ORDER BY 2,4";
		} else {
			// finding components associated with applications
			System.out.println("Doing components for apps componentid="+comp.getId());
			if (isRelease)
				sql = "SELECT	a.id,a.name,a.parentid,c.id,c.name,c.parentid	"
						+	"FROM	dm.dm_component			c,	"
						+	"dm.dm_application		a,	"
						+	"dm.dm_applicationcomponent	x	"
						+	"WHERE	 a.status = 'N' and c.status = 'N' and ? in (c.id,c.parentid) "
						+	"AND	x.childappid=c.id		"
						+	"AND	a.id=x.appid	"
						+	"order by 2,4";
			else
			   sql = "SELECT a.id,a.name,a.parentid,c.id,c.name,c.parentid "
			     + "FROM dm.dm_component   c, "
			     + "dm.dm_application  a, "
			     + "dm.dm_applicationcomponent x "
			     + "WHERE a.status = 'N' and c.status = 'N' and ? in (c.id,c.parentid)   "
			     + "AND x.compid=c.id  "
			     + "AND a.id=x.appid "
			     + "order by 2,4"; 
		}
		try
		{
			System.out.println(sql);
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			if (bServers) {
				stmt.setInt(1, comp.getId());
				stmt.setInt(2, comp.getId());
				stmt.setInt(3, comp.getId());
				stmt.setInt(4, comp.getId());
			} else {
				stmt.setInt(1, comp.getId());
			}
			ResultSet rs = stmt.executeQuery();
			TableDataSet ret = new TableDataSet(5);
			int row = 0;
			while(rs.next()) {
				if (bServers) {
					// Creating table for components on servers
					System.out.println("Outputting table data for servers");
					ret.put(row, 0, new Environment(this, rs.getInt(1), rs.getString(2)).getLinkJSON());
					int servid = getInteger(rs, 3, 0);
					if(servid != 0) {
						Server s = new Server(this, servid, rs.getString(4));
						ret.put(row, 1, s.getLinkJSON());
						Component c = new Component(this,rs.getInt(5),rs.getString(6));
						c.setParentId(getInteger(rs,7,0));
						ret.put(row, 2, c.getLinkJSON());
						int deploymentid = getInteger(rs, 8, 0);
						if(deploymentid != 0) {
							Deployment d = new Deployment(this, deploymentid, rs.getInt(9));
							d.setFinished(rs.getInt(11));
							ret.put(row, 3, d.getLinkJSON());
						} else {
							int dt = getInteger(rs,15,0);
							System.out.println("dt="+dt);
							if (dt>0) {
								ret.put(row, 3, new CreatedModifiedField(
										formatDateToUserLocale(rs.getInt(15)),
										new User(this, rs.getInt(12), rs.getString(13), rs.getString(14))));	
							}
						}
						int buildnumber = getInteger(rs, 10, 0);
						ret.put(row, 4, buildnumber);
					}
				} else {
					System.out.println("Outputting table data for apps");
					Application a = new Application(this,rs.getInt(1),rs.getString(2));
					a.setParentId(getInteger(rs,3,0));
					ret.put(row, 0, a.getLinkJSON());
					Component c = new Component(this,rs.getInt(4),rs.getString(5));
					c.setParentId(getInteger(rs,6,0));
					ret.put(row, 1, c.getLinkJSON());
				}
				row++;
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve component location info for component '" + comp.getName() + "' from database");
	}

	
	private void AddAttribute(List <DMAttribute> ret,String Name,String Value)
	{
		DMAttribute dmattribute = new DMAttribute();
		dmattribute.setName(Name);
		dmattribute.setValue(Value);
		ret.add(dmattribute);
	}
	
	// TODO: This is Summary data - it has been replaced with getSummaryJSON on DMObject
	public List<DMAttribute> getAttributes(String ObjectType,int id)
	{
		List <DMAttribute> ret = new ArrayList<DMAttribute>();
		try
		{
			if (ObjectType.compareToIgnoreCase("app")==0)
			{
				// Getting details for an application
				String sql="select name,summary,actionid,ownerid,ogrpid from dm.dm_application where id=?";
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1, id);
				ResultSet rs = stmt.executeQuery();
				if (rs.next())
				{
					AddAttribute(ret,"Name",rs.getString(1));
					AddAttribute(ret,"Summary",rs.getString(2));
				}
				rs.close();
				return ret;
			}
			else
			if (ObjectType.compareToIgnoreCase("comp")==0)
			{
				// Getting details for a component
				String sql="select name,summary from dm.dm_component where id=?";
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1, id);
				ResultSet rs = stmt.executeQuery();
				if (rs.next())
				{
					AddAttribute(ret,"Name",rs.getString(1));
					AddAttribute(ret,"Summary",rs.getString(2));
				}
				rs.close();
				return ret;
			}
			else
			if (ObjectType.compareToIgnoreCase("compitem")==0)
			{
				// Getting details for a component item
				String sql="select b.name from dm.dm_componentitem a,dm.dm_repository b where a.id=? and b.id=a.repositoryid";
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1, id);
				ResultSet rs = stmt.executeQuery();
				if (rs.next())
				{
					AddAttribute(ret,"Repository Name",rs.getString(1));
				}
				rs.close();
				//
				// Now the variable attributes (overrides based on repository type)
				//
				System.out.println("second sql");
				String sql2="SELECT	name,value FROM	dm.dm_compitemprops WHERE compitemid=?";
				PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
				stmt2.setInt(1, id);
				ResultSet rs2 = stmt2.executeQuery();
				while (rs2.next())
				{
					AddAttribute(ret,rs2.getString(1),rs2.getString(2));
				}
				rs.close();
				return ret;
			}
		}
		
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve object " +ObjectType+" from database");
	}
	
	
	public List<ComponentItem> getComponentItems(int compid)
	{
		List <ComponentItem> ret = new ArrayList<ComponentItem>();
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery(
					"SELECT	a.id, a.name, a.target, a.xpos, a.ypos, a.predecessorid, a.summary, a.rollup, a.rollback			" +
					"FROM	dm.dm_componentitem		a			" +
					"WHERE	a.compid="+compid);
			while (rs.next())
			{
				ComponentItem ci = new ComponentItem();
				ci.setId(rs.getInt(1));
				ci.setName(rs.getString(2));
				ci.setTargetDir(rs.getString(3));
				ci.setXpos(rs.getInt(4));
				ci.setYpos(rs.getInt(5));
				ci.setPredecessorId(rs.getInt(6));
				ci.setSummary(rs.getString(7));
				ci.setRollup(ComponentFilter.fromInt(getInteger(rs, 8, 0)));
				ci.setRollback(ComponentFilter.fromInt(getInteger(rs, 9, 0)));
				ret.add(ci);
			}
			rs.close();
			return ret;
		}
		
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve component items from database");
	}
	
	
	public boolean isReadable(int ObjectType) {
		return true;
	}
	
	public boolean isWriteable(int ObjectType) {
		return true;
	}
	
	public boolean isCreateable(int ObjectType) {
		String Create="";
		String sql = 	"SELECT	a.cperm			" +
						"FROM	dm_privileges	" + 
						"WHERE	object_type=?	" +
						"AND	userid=?		";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,ObjectType);
			stmt.setInt(2,getUserID());
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
			{
				// found a result - ignore the group
				Create = rs.getString(1);
			}
			else
			{
				// Didn't get anything for user - try group
				sql = 	"SELECT a.cperm							" +
						"FROM	dm_privileges		a,			" +
						"		dm_usersingroup		b,			" +
						"		dm_user				c			" +
						"WHERE	a.object_type=?					" +
						"AND	a.groupid = b.groupid			" +
						"AND	b.userid = ?					";

				PreparedStatement stmt2 = getDBConnection().prepareStatement(sql);
				stmt2.setInt(1,ObjectType);
				stmt2.setInt(2,getUserID());
				ResultSet rs2 = stmt.executeQuery();
				if (rs2.next())
				{
					// found a result - ignore the group
					Create = rs2.getString(1);
				}
				stmt2.close();
			}
			stmt.close();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return (Create.charAt(0) == 'Y' || Create.charAt(0)=='y');
	}
	
	public boolean userOwnsEnvironment(int envid)
	{
		Environment env = getEnvironment(envid,true);
		boolean envowner=false;
		DMObject owner = env.getOwner();
		if (owner != null) {
			System.out.println("Environment has an owner");
			// Environment has an owner
			if (owner.getObjectType() == ObjectType.USERGROUP) {
				System.out.println("Owner is a usergroup");
				// Owner is a group - we're the "owner" if we're a member of this group
				UserList users = getUsersInGroup(owner.getId());
				for (User user: users) {
					if (user.getId()==getUserID()) {
						envowner=true;
						break;
					}
				}
			} else {
				// Owner must be a user
				System.out.println("Owner is a user m_userID="+getUserID()+" owner.getId()="+owner.getId());
				envowner = (getUserID() == owner.getId());
			}
		} else {
			System.out.println("No owner - setting envowner to true");
			// No owner - just allow people to create entries
			envowner=true;
		}
		return envowner;
	}
	
	public void AddRequestEntry(int id,int appid,String Description,boolean cal)
	{
		String sql;
		if (cal) {
			sql="INSERT INTO dm.dm_request(id,userid,"+whencol+",note,calendarid,appid,status) VALUES(?,?,?,?,?,?,'N')";
		} else {
			sql="INSERT INTO dm.dm_request(id,userid,"+whencol+",note,taskid,appid,status) VALUES(?,?,?,?,?,?,'N')";
		}
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,getID("request"));
			stmt.setInt(2,getUserID());
			stmt.setLong(3,timeNow());
			stmt.setString(4,Description);
			stmt.setInt(5,id);
			stmt.setInt(6,appid);
			stmt.execute();
			getDBConnection().commit();
			stmt.close();
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
	}
	
	public void AddPendingRequest(int id,int appid,String Description)
	{
		AddRequestEntry(id,appid,Description,true);
	}
	
	public int AddEvent(DMCalendarEvent e)
	{
		//
		// TO DO : Check Permissions
		// TO DO: If insert is "P"ending then also put an entry into the requests table.
		String sql = 	"INSERT INTO dm.dm_calendar(id,envid,eventname,eventtype,starttime,endtime,allday,status,appid,description,creatorid,created,modifierid,modified) " +
						"VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		try
		{
			boolean envowner=userOwnsEnvironment(e.getEnvID());
			System.out.println("In AddEvent, envowner="+envowner);
			String status=envowner?"N":"P";	// Normal (we are the environment owner) or Pending 
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			int id = getID("calendar");
			long t = timeNow();
			String EventType = e.getEventTypeString().toString();
			EventType = Character.toUpperCase(EventType.charAt(0)) + EventType.substring(1).toLowerCase();
			System.out.println("EventType="+EventType);
			stmt.setInt(1,id);
			stmt.setInt(2,e.getEnvID());
			stmt.setString(3,e.getEventTitle());
			stmt.setString(4,EventType);
			stmt.setInt(5, e.getStart());
			stmt.setInt(6, e.getEnd());
			stmt.setString(7, e.getAllDayEvent()?"Y":"N");
			stmt.setString(8, status);
			int appid = e.getAppID();
			if(appid != 0) {
				stmt.setInt(9, appid);
			} else {
				stmt.setNull(9, Types.INTEGER);
			}
			stmt.setString(10, e.getEventDesc());
			System.out.println("getEventDesc="+e.getEventDesc());
			stmt.setInt(11,e.getCreatorID());
			stmt.setLong(12,t);
			stmt.setInt(13,e.getCreatorID());
			stmt.setLong(14,t);
			boolean res = stmt.execute();
			System.out.println("res="+(res?"true":"false"));
			stmt.close();
			Environment env = getEnvironment(e.getEnvID(),false);
			RecordObjectUpdate(env,"Calendar Entry <a href='javascript:SwitchToCalendar("+id+");'>\""
			+e.getEventTitle()
			+(status.equalsIgnoreCase("p")?"\"</a> Requested":"\"</a> Created"));
			getDBConnection().commit();
			return id;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return 0;
	}
	
	public int DeleteEvent(int eventid)
	{
		//
		// TO DO: Check Permissions
		//
		String sql1 = 	"UPDATE dm.dm_calendar SET status='D' WHERE id=?";
		String sql2 = 	"UPDATE dm.dm_request SET status='C', completed=? where calendarid=?";
		String sql3 =   "SELECT envid,eventname FROM dm.dm_calendar WHERE id=?";
 		try
		{
 			int res=0;
 			PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
 			stmt3.setInt(1,eventid);
 			ResultSet rs3 = stmt3.executeQuery();
 			if (rs3.next()) {
 				Environment env = getEnvironment(rs3.getInt(1),false);
				PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
				stmt1.setInt(1,eventid);
				stmt1.execute();
				res = stmt1.getUpdateCount();
				stmt1.close();
				PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
				stmt2.setLong(1,timeNow());
				stmt2.setInt(2,eventid);
				stmt2.execute();
				RecordObjectUpdate(env,"Event \""+rs3.getString(2)+"\" Deleted");
				getDBConnection().commit();
 			}
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return -1;
	}
	
	private DMEventType getEventType(String et)
	{
		if (et.equalsIgnoreCase("reserved")) return DMEventType.RESERVED;
		if (et.equalsIgnoreCase("unavailable")) return DMEventType.UNAVAILABLE;
		if (et.equalsIgnoreCase("auto")) return DMEventType.AUTO;
		System.out.println("Returning NoEvent");
		return DMEventType.NOEVENT;
	}
	
	
	
	public DMCalendarEvent getCalendarEvent(int eventid)
	{
		DMCalendarEvent ret = new DMCalendarEvent();
		ret.setID(eventid);
		String sql = 	"SELECT envid,eventname,rtrim(eventtype),starttime,endtime, "
				+		"appid,description,allday,status "
				+		"FROM dm.dm_calendar WHERE id=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,eventid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				ret.setEnvID(rs.getInt(1));
				ret.setEventTitle(rs.getString(2));
				System.out.println("event type = "+rs.getString(3));
				ret.setEventType(getEventType(rs.getString(3)));
				DMEventType t = getEventType(rs.getString(3));
				
				System.out.println("event type stored = "+ret.getEventTypeString()+" or "+t.toString());
				ret.setStart(rs.getInt(4));
				ret.setEnd(rs.getInt(5));
				ret.setAppID(rs.getInt(6));
				ret.setEventDesc(rs.getString(7));
				ret.setAllDayEvent(rs.getString(8).equalsIgnoreCase("y"));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}
	
	public int ModifyEvent(DMCalendarEvent ce)
	{
		//
		// TO DO: Check Permissions
		//
		String sql = "UPDATE dm.dm_calendar SET eventname=?, eventtype=?,starttime=?, endtime=?, appid=?, description=?, allday=?, modifierid=?, modified=?  WHERE id=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setString(1,ce.getEventTitle());
			stmt.setString(2,ce.getEventTypeString());
			stmt.setInt(3,ce.getStart());
			stmt.setInt(4,ce.getEnd());
			stmt.setInt(5,ce.getAppID());
			stmt.setString(6,ce.getEventDesc());
			stmt.setString(7,ce.getAllDayEvent()?"Y":"N");
			stmt.setInt(8,getUserID());
			stmt.setLong(9,timeNow());
			stmt.setInt(10,ce.getID());
			System.out.println("id to update = "+ce.getID());
			stmt.execute();
			int res = stmt.getUpdateCount();
			System.out.println("UpdateCount="+stmt.getUpdateCount()+" ce.getEventTitle=["+ce.getEventTitle()+"]");
			stmt.close();
			Environment env = getEnvironment(ce.getEnvID(),false);
			this.RecordObjectUpdate(env,"Calendar Entry \""+ce.getEventTitle()+"\" Changed");
			getDBConnection().commit();
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return -1;
	}
	
	public int ApproveEvent(int eventid)
	{
		//
		// TO DO: Check Permissions
		//
		String sql1 = "UPDATE dm.dm_calendar SET status='N' where id=?";
		String sql2 = "UPDATE dm.dm_request SET status='C', completed=? where calendarid=?";
		String sql3 = "SELECT envid,eventname FROM dm.dm_calendar WHERE id=?";
		try
		{
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,eventid);
			stmt1.execute();
			int res = stmt1.getUpdateCount();
			System.out.println("UpdateCount="+stmt1.getUpdateCount());
			stmt1.close();
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			stmt2.setLong(1,timeNow());
			stmt2.setInt(2,eventid);
			stmt2.execute();
			PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
			stmt3.setInt(1,eventid);
			ResultSet rs3 = stmt3.executeQuery();
			if (rs3.next()) {
				Environment env = getEnvironment(rs3.getInt(1),false);
				this.RecordObjectUpdate(env, "Calendar Event \""+rs3.getString(2)+"\" Approved");
			}
			rs3.close();
			getDBConnection().commit();
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return -1;
	}
	
	public List<DMCalendarEvent> getEvents(Environment env,Application app,long starttime,long endtime)
	{
		// Called from API - list all events
		List <DMCalendarEvent> ret = new ArrayList<DMCalendarEvent>();
		String sql1 = 	"SELECT c.id,c.envid,c.eventname,rtrim(c.eventtype),c.starttime,c.endtime,	"
				+	"c.allday,c.status,c.appid,c.description,c.creatorid,c.created,c.modifierid,c.modified, "
				+	"r.completed, r.completedby "
				+	"FROM dm.dm_calendar c "
				+	"LEFT OUTER JOIN dm.dm_request r ON r.calendarid = c.id "
				+	"LEFT OUTER JOIN dm.dm_environment e on e.id = c.envid "
				+	"LEFT OUTER JOIN dm.dm_application a on a.id = c.appid "
				+	"WHERE c.status<>'D' AND e.domainid in ("+m_domainlist+") "
				+	"AND a.domainid in ("+m_domainlist+") AND starttime>=?";
		int ebv=0,abv=0,tbv=0;
		int nbv=2;
		if (env != null) {
			sql1=sql1+" AND c.envid=?";
			ebv=nbv++;
		}
		if (app != null) {
			sql1=sql1+" AND c.appid=?";
			abv=nbv++;
		}
		if (endtime>0) {
			sql1=sql1+" AND c.endtime<=?";
			tbv=nbv++;
		}
		// String sql2 = 	"SELECT envid,unavailstart,unavailend FROM dm.dm_availability WHERE envid=?";
		String sql3 = 	"SELECT deploymentid,userid,exitcode,exitstatus,appid,started,finished FROM dm.dm_deployment " +
						"WHERE started>=? AND finished<=?";
		if (env != null) sql3=sql3+" AND envid=?";
		if (app != null) sql3=sql3+" AND appid=?";
		try
		{
			System.out.println("sql1="+sql1);
			PreparedStatement stmt = getDBConnection().prepareStatement(sql1);
			stmt.setLong(1,starttime);
			if (env != null) stmt.setInt(ebv,env.getId());
			if (app != null) stmt.setInt(abv,app.getId());
			if (endtime>0) stmt.setLong(tbv,endtime);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				DMCalendarEvent ce = new DMCalendarEvent();
				String status = rs.getString(8);
				String EventType = rs.getString(4);		// TODO: This needs to be used!
				if (status != null && status.equalsIgnoreCase("p")) {
					// Pending event
					ce.setPending(true);
				} else {
					ce.setPending(false);
				}
				if (EventType.equalsIgnoreCase("reserved")) ce.setEventType(DMEventType.RESERVED);
				else
				if (EventType.equalsIgnoreCase("unavailable")) ce.setEventType(DMEventType.UNAVAILABLE);
				else
				if (EventType.equalsIgnoreCase("auto")) ce.setEventType(DMEventType.AUTO);
				ce.setID(rs.getInt(1));
				ce.setEnvID(rs.getInt(2));
				ce.setEventTitle(getString(rs, 3, ""));
				int eventStart = rs.getInt(5);
				int eventEnd = rs.getInt(6);
				ce.setStart(eventStart);
				if (eventEnd < eventStart+900) {
					eventEnd = eventEnd + (eventEnd % 900);
				}
				ce.setEnd(eventEnd);
				ce.setAllDayEvent(getBoolean(rs,7,false));
				
				ce.setAppID(rs.getInt(9));
				ce.setEventDesc(getString(rs, 10, ""));
				ce.setCreatorID(rs.getInt(11));
				ce.setCreated(rs.getInt(12));
				ce.setModifierID(rs.getInt(13));
				ce.setModified(rs.getInt(14));
				ce.setApproved(getInteger(rs,15,0));
				ce.setApprover(getInteger(rs,16,0));
				ret.add(ce);
			}
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve calendar events from database");
		
	}
	
	List<DMCalendarEvent> getCalendarEvents(int envid,int start,int end)
	{
		List <DMCalendarEvent> ret = new ArrayList<DMCalendarEvent>();
		String sql1 = 	"SELECT c.id,c.envid,c.eventname,rtrim(c.eventtype),c.starttime,c.endtime,	"
					+	"c.allday,c.status,c.appid,c.description,c.creatorid,c.created,c.modifierid,c.modified, "
					+	"r.completed, r.completedby "
					+	"FROM dm.dm_calendar c "
					+	"LEFT OUTER JOIN dm.dm_request r ON r.calendarid = c.id "
					+	"LEFT OUTER JOIN dm.dm_application a ON a.id = c.appid "
					+	"WHERE ((starttime>=? AND starttime<=?) OR (endtime>=? AND endtime<=?)) AND envid=? AND c.status <>'D' AND (a.status is null or a.status='N')";
		String sql2 = 	"SELECT envid,unavailstart,unavailend FROM dm.dm_availability WHERE envid=?";
		String sql3 = 	"SELECT d.deploymentid,d.userid,d.exitcode,d.exitstatus,d.appid,d.started,d.finished FROM dm.dm_deployment d,dm.dm_application a " +
						"WHERE d.started>=? AND d.finished<=? AND d.envid=? AND d.appid=a.id AND a.status='N'";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql1);
			stmt.setInt(1,start);
			stmt.setInt(2,end);
			stmt.setInt(3,start);
			stmt.setInt(4,end);
			stmt.setInt(5,envid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				DMCalendarEvent ce = new DMCalendarEvent();
				String status = rs.getString(8);
				String EventType = rs.getString(4);		// TODO: This needs to be used!
				if (status != null && status.equalsIgnoreCase("p")) {
					// Pending event
					ce.setPending(true);
				} else {
					ce.setPending(false);
				}
				if (EventType.equalsIgnoreCase("reserved")) ce.setEventType(DMEventType.RESERVED);
				else
				if (EventType.equalsIgnoreCase("unavailable")) ce.setEventType(DMEventType.UNAVAILABLE);
				else
				if (EventType.equalsIgnoreCase("auto")) ce.setEventType(DMEventType.AUTO);
				ce.setID(rs.getInt(1));
				ce.setEnvID(rs.getInt(2));
				ce.setEventTitle(getString(rs, 3, ""));
				int eventStart = rs.getInt(5);
				int eventEnd = rs.getInt(6);
				ce.setStart(eventStart);
				if (eventEnd < eventStart+900) {
					eventEnd = eventEnd + (eventEnd % 900);
				}
				ce.setEnd(eventEnd);
				ce.setAllDayEvent(getBoolean(rs,7,false));
				
				ce.setAppID(rs.getInt(9));
				ce.setEventDesc(getString(rs, 10, ""));
				ce.setCreatorID(rs.getInt(11));
				ce.setCreated(rs.getInt(12));
				ce.setModifierID(rs.getInt(13));
				ce.setModified(rs.getInt(14));
				ce.setApproved(getInteger(rs,15,0));
				ce.setApprover(getInteger(rs,16,0));
				ret.add(ce);
			}
			rs.close();
			//
			// Now overlay the availability events from the weekly availability calendar
			//
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			stmt2.setInt(1,envid);
			ResultSet rs2 = stmt2.executeQuery();
			int x=1;
			while (rs2.next())
			{
				DMCalendarEvent ce = new DMCalendarEvent();
				ce.setAllDayEvent(false);
				ce.setID(-1-x);	// negative id = weekly "unavailable" event
				ce.setEnvID(rs2.getInt(1));
				ce.setEventTitle("Unavailable");
				ce.setStart(rs2.getInt(2)+start);	// Start is start of week
				ce.setEnd(rs2.getInt(3)+start);
				ce.setAppID(0);
				ce.setEventDesc("");
				ce.setEventType(DMEventType.UNAVAILABLE);
				ret.add(ce);
				x++;
			}
			rs2.close();
			//
			// Now do any historic deployments
			//
			PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
			stmt3.setInt(1,start);
			stmt3.setInt(2,end);
			stmt3.setInt(3,envid);
			ResultSet rs3 = stmt3.executeQuery();
			while (rs3.next())
			{
				DMCalendarEvent ce = new DMCalendarEvent();
				ce.setAllDayEvent(false);
				ce.setID(rs3.getInt(1));	// need to differentiate this from normal calendar events
				ce.setEnvID(envid);
				ce.setEventTitle("Deployed");
				int depStart = rs3.getInt(6);
				int depEnd = rs3.getInt(7);
				int interval = depEnd-depStart;
				System.out.println("depstart="+depStart+" depend="+depEnd+" interval="+interval);
				ce.setStart(depStart);
				if (interval < 900) {
					// interval between start & end should be at LEAST 900 secs (15 mins) and end on a 15 minute boundary
					interval = 900 + (interval % 900);
					System.out.println("Adding "+interval+" to depEnd");
					depEnd = depEnd + interval;
					System.out.println("depEnd now "+depEnd);
				}
				ce.setEnd(depEnd);
				ce.setAppID(rs3.getInt(5));
				ce.setEventDesc(rs3.getString(4));
				ce.setEventType(DMEventType.DEPLOYMENT);
				ce.setDeployID(rs3.getInt(1));
				ret.add(ce);
				x++;
			}
			rs3.close();
			
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve calendar events from database");
	}
	
	
	List<DMCalendarEvent> getWeeklySchedule(int envid)
	{
		List <DMCalendarEvent> ret = new ArrayList<DMCalendarEvent>();
		String sql = "SELECT unavailstart,unavailend FROM dm.dm_availability WHERE envid=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,envid);
			ResultSet rs = stmt.executeQuery();
			int x=1;
			while (rs.next())
			{
				DMCalendarEvent ce = new DMCalendarEvent();
				ce.setAllDayEvent(false);
				ce.setID(0-x);
				ce.setEnvID(rs.getInt(1));
				ce.setEventTitle("Unavailable");
				ce.setEventType(DMEventType.UNAVAILABLE);
				ce.setStart(rs.getInt(1)+345600);
				ce.setEnd(rs.getInt(2)+345600);
				ret.add(ce);
				x++;
			}
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve availability events from database");
	}
	
	
	public void DeleteAllAvailability(int envid)
	{
		String sql = "DELETE FROM dm.dm_availability WHERE envid=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,envid);
			stmt.execute();
			stmt.close();
			getDBConnection().commit();
			return;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to delete availability events from database");
	}
	
	private void UpdateCalendarAttributes(int envid,String colname,int t,String newval)
	{
		String sql = "UPDATE dm.dm_environment SET "+colname+"=? WHERE id=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			if (t==1) {
				stmt.setInt(1,Integer.parseInt(newval));
			} else {
				stmt.setString(1,newval);
			}
			stmt.setInt(2,envid);
			stmt.execute();
			stmt.close();
			getDBConnection().commit();
			return;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to update calendar attributes colname="+colname+" newval="+newval);
	}
	
	public void setCalendarStartTime(int envid,int timeinmins)
	{
		UpdateCalendarAttributes(envid,"calstart",1,Integer.toString(timeinmins));
	}

	public void setCalendarEndTime(int envid, int timeinmins)
	{
		UpdateCalendarAttributes(envid,"calend",1,Integer.toString(timeinmins));
	}

	public void setCalendarAvailability(int envid,String na)
	{
		UpdateCalendarAttributes(envid,"calusage",2,na);
	}
	
	public void AddAvailability(int envid,int start,int end)
	{
		String sql = "INSERT INTO dm.dm_availability(envid,unavailstart,unavailend) VALUES(?,?,?)";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,envid);
			stmt.setInt(2,start);
			stmt.setInt(3,end);
			stmt.execute();
			stmt.close();
			getDBConnection().commit();
			return;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to insert availability event into database");
	}
	
	public String GetApplicationNameFromID(int appid)
	{
		String ret="";
		String sql = "SELECT name FROM dm.dm_application WHERE id=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,appid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
			{
				ret = rs.getString(1);
			}
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve application name from database");
	}
	
	//public boolean approveApplication(Application app, Domain tgtdomain, boolean approve, String note)
	//{
	//	if(approve) {
	//		System.out.println("Approving application "+app.getId()+" for domain "+tgtdomain.getId());
	//	} else {
	//		System.out.println("Rejecting application "+app.getId()+" for domain "+tgtdomain.getId());
	//	}

	//	int id = getID("Approval");
	//	if(id == 0) {
	//		return false;
	//	}
	//	
	//	String sql = "INSERT INTO dm.dm_approval(id, appid, \"when\", userid, approved, note) VALUES (?,?,?,?,?,?)"; 
	//	try {
	//		PreparedStatement stmt = m_conn.prepareStatement(sql);
	//		stmt.setInt(1, id);
	//		stmt.setInt(2, app.getId());
	//		stmt.setLong(3, timeNow());
	//		stmt.setInt(4, m_userID);
	//		stmt.setString(5, (approve ? "Y" : "N"));
	//		stmt.setString(6, note);
	//		stmt.execute();
	//		stmt.close();
	//		m_conn.commit();
	//		return true;
	//	} catch(SQLException e) {
	//		e.printStackTrace();
	//		rollback();
	//	}		
	//	return false;
	//}
	
	public List <Application> GetApplicationsInEnvironment(int envid, boolean isRelease)
	{
		List <Application> ret = new ArrayList<Application>();
		
		Environment env = this.getEnvironment(envid, true);
		String domainlist = "";
		Domain lifeCycleDomain = null;
  
		domainlist += env.getDomainId() + ",";
  
		Domain d = env.getDomain();
		while (d != null) {
			domainlist += d.getDomainId() + ",";
			int id = d.getDomainId();
			d = getDomain(id);
			if (d != null && d.getLifecycle()) lifeCycleDomain = d;
		}
		if (lifeCycleDomain != null) {
			// Need to add sibling domains
			List<Domain> children = getChildDomains(lifeCycleDomain);
			for (Domain cd: children) {
				domainlist += cd.getId() + ",";
			}
			
		}
		domainlist = domainlist.replaceAll(",$", "");
		
		System.out.println("domainlist="+domainlist);
		
		String sql;
		if (isRelease) {
			sql = "SELECT a.id,a.name,a.domainid FROM dm.dm_application a "
				+ "WHERE a.isRelease = 'Y' and a.domainid in (" + domainlist + ") AND a.status='N' ORDER BY 2";
		} else {
			sql = "SELECT a.id,a.name,a.domainid FROM dm.dm_application a, dm.dm_appsallowedinenv b "
				+ "WHERE b.envid=? AND (a.id=b.appid OR a.parentid=b.appid) AND a.isRelease <> 'Y' and a.domainid in (" + domainlist + ") AND a.status='N' ORDER BY 2";
		}
		
		System.out.println("sql="+sql);
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			if (!isRelease)	stmt.setInt(1,envid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Application app = getApplication(rs.getInt(1),true);
				System.out.println("got app "+app.getName());
				//ce.setId(rs.getInt(1));
				//ce.setName(rs.getString(2));
				//Domain d2 = getDomain(rs.getInt(3));
				//ce.setDomain(d2);
				ret.add(app);
			}
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve applications in environment from database");
	}
	
	// Build Engine
	public Builder getBuilder(int builderid)
	{
		if (builderid < 0) {
			Builder ret = new Builder(this, builderid, "");
			ret.setName("");
			ret.setSummary("");
			return ret;
		}
		String sql =  "SELECT	b.name,b.summary,b.domainid,b.status,b.credid, "
				 	+ "			uc.id, uc.name, uc.realname, b.created, "
					+ "			um.id, um.name, um.realname, b.modified, "
					+ "			uo.id, uo.name, uo.realname, g.id, g.name "
					+ "FROM		dm.dm_buildengine b  "
					+ "LEFT OUTER JOIN dm.dm_user uc ON b.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON b.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_user uo ON b.ownerid = uo.id "			// owner user
					+ "LEFT OUTER JOIN dm.dm_usergroup g ON b.ogrpid = g.id "		// owner group
					+ "WHERE	b.id=?";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, builderid);
			ResultSet rs = stmt.executeQuery();
			Builder ret = null;
			if(rs.next()) {
				ret = new Builder(this, builderid, rs.getString(1));
				ret.setSummary(rs.getString(2));
				ret.setDomainId(getInteger(rs, 3, 0));
				getStatus(rs, 4, ret);
				int credid = getInteger(rs,5,0);
				if (credid != 0) {
					Credential cred = getCredential(credid,false);
					ret.setCredential(cred);
				}
				getCreatorModifierOwner(rs, 6, ret);
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
		// throw new RuntimeException("Unable to retrieve builder " + builderid + " from database");	
	}
	// Build Job
	public BuildJob getBuildJob(int buildjobid)
	{
		if (buildjobid < 0) {
			BuildJob ret = new BuildJob(this, buildjobid, "");
			ret.setName("");
			ret.setSummary("");
			return ret;
		}
		String sql =  "SELECT	b.name,b.summary,b.builderid,b.status,b.projectname, "
				 	+ "			uc.id, uc.name, uc.realname, b.created, "
					+ "			um.id, um.name, um.realname, b.modified "
					+ "FROM		dm.dm_buildjob b  "
					+ "LEFT OUTER JOIN dm.dm_user uc ON b.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON b.modifierid = um.id "		// modifier
					+ "WHERE	b.id=?";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, buildjobid);
			ResultSet rs = stmt.executeQuery();
			BuildJob ret = null;
			if(rs.next()) {
				ret = new BuildJob(this, buildjobid, rs.getString(1));
				ret.setSummary(rs.getString(2));
				ret.setBuilderId(getInteger(rs, 3, 0));
				getStatus(rs, 4, ret);
				ret.setProjectName(rs.getString(5));
				getCreatorModifier(rs, 6, ret);
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
			
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve build job " + buildjobid + " from database");	
	}
	
	// Environment
	
	public Environment getEnvironment(int envid, boolean detailed)
	{
	 if (envid < 0)
	 {
	  Environment ret = new Environment(this, envid, "");
	  ret.setName("");
	  ret.setSummary("");
	  return ret;
	 }
	 
		String sql = null;
		if(detailed) {
			sql = "SELECT e.name, e.summary, e.domainid, e.status, e.calstart, e.calend, e.calusage, "
				+ "  uc.id, uc.name, uc.realname, e.created, "
				+ "  um.id, um.name, um.realname, e.modified, "
				+ "  uo.id, uo.name, uo.realname, g.id, g.name "
				+ "FROM dm.dm_environment e "
				+ "LEFT OUTER JOIN dm.dm_user uc ON e.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON e.modifierid = um.id "		// modifier
				+ "LEFT OUTER JOIN dm.dm_user uo ON e.ownerid = uo.id "			// owner user
				+ "LEFT OUTER JOIN dm.dm_usergroup g ON e.ogrpid = g.id "		// owner group
				+ "WHERE e.id = ?";	
		} else {
			sql = "SELECT e.name, e.summary, e.domainid, e.status, e.calstart, e.calend, e.calusage "
				+ "FROM dm.dm_environment e WHERE e.id = ?";	
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, envid);
			ResultSet rs = stmt.executeQuery();
			Environment ret = null;
			if(rs.next()) {
				ret = new Environment(this, envid, rs.getString(1));
				ret.setSummary(rs.getString(2));
				ret.setDomainId(getInteger(rs, 3, 0));
				getStatus(rs, 4, ret);
				ret.setCalStart(getInteger(rs,5,0));
				ret.setCalEnd(getInteger(rs,6,1440)); 	// 00:00 in mins (1,440 mins = full day)
				ret.setCalUsage(getString(rs,7,"E"));	// (E)xcept for Calendar Usage
				if(detailed) {
					getCreatorModifierOwner(rs, 8, ret);
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve environment " + envid + " from database");				
	}
	
	// Used by API only
	

	
	private int getDomainID2(String DomainName,int parent)
	{
		//
		// Domain ID can be fully qualified. (DOMAIN.DOMAIN.DOMAIN...)
		// Since individual domain names need not be unique (UAT can exist in
		// more than one parent domain for example) then we need to make sure
		// that we return an error if more than one is found.
		//
		System.out.println("getDomainID2(\""+DomainName+"\","+parent+")");
		int domid=DOMAIN_NOT_FOUND;
		int dot = DomainName.indexOf('.');
		if (dot >= 0)
		{
			System.out.println("dot="+dot);
			// DomainName is full qualified
			String dn1 = DomainName.substring(0,dot);
			String dn2 = DomainName.substring(dot+1);
			System.out.println("dn1=["+dn1+"] dn2=["+dn2+"]");
			int p2 = getDomainID2(dn1,parent);	// This should return the ID of the first bit
			if (p2>=0)
			{
				// So far, so good...
				if (dn2.length()>0)
				{
					domid = getDomainID2(dn2,p2);
					if (domid<0) {
						// There's been an error - bail out
						return domid;
					}
				}
			}
			else
			{
				return p2;
			}
		}
		else
		{
			String sql = "SELECT id from dm.dm_domain where id in (" + m_domainlist + ") and name=?";
			if (parent > 0) {
				sql = sql + " AND domainid=?";
			}
			try
			{
				System.out.println("sql="+sql);
				System.out.println("DomainName=["+DomainName+"]");
				System.out.println("parent="+parent);
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setString(1, DomainName);
				if (parent > 0) stmt.setInt(2,parent);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					// got at least one row. 
					domid = rs.getInt(1);
					// Check we're unique
					if (rs.next()) {
						// Not unique
						domid=DOMAIN_OBJECT_AMBIGUOUS;
					}
				} else {
					// No row returned - cannot find this domain
					domid=DOMAIN_NOT_FOUND;
				}
				rs.close();
				stmt.close();
				
			}
			catch(SQLException ex)
			{
				ex.printStackTrace();
				rollback();
				throw new RuntimeException("Unable to retrieve domain from database");
			}
		}
		return domid;
	}

	public int getDomainID(String DomainName)
	{
		return getDomainID2(DomainName,0);
	}
	
	private int getObjectDomainId(String objName)
	{
		int dot = objName.lastIndexOf('.');
		int domainid=DOMAIN_NOT_SPECIFIED;
		if (dot > 0) {
			String dn = objName.substring(0,dot);
			System.out.println("dn="+dn);
			domainid = getDomainID(dn);
			switch(domainid) {
			case DOMAIN_NOT_FOUND:
				throw new RuntimeException("Domain "+dn+" Not Found or No Access");
			case DOMAIN_OBJECT_AMBIGUOUS:
				throw new RuntimeException("Domain "+dn+" is Ambiguous");
			}
		}
		return domainid;
	}
	
	private DMObject getObjectByName(ObjectType ot,String name)
	{
		String table_name="dm_"+ot.toString().toLowerCase();
		String sql;
		int domainid = getObjectDomainId(name);
		if (domainid == DOMAIN_NOT_SPECIFIED) {
			if (ot == ObjectType.TEMPLATE) {
				// Templates don't have domains of their own
				sql = "SELECT t.id FROM dm.dm_template t,dm_notify n WHERE t.notifierid=n.id AND n.domainid in (" + m_domainlist + ") and t.name = ? AND t.status='N'";
			}
			else
			if (ot == ObjectType.BUILDJOB) {
				// Build Jobs don't have domains of their own
				sql = "SELECT b.id FROM dm.dm_buildengine b,dm_buildjob j WHERE j.builderid=b.id AND b.domainid in (" + m_domainlist + ") and j.name = ? AND j.status='N'";

			} else {
				sql = "SELECT id FROM dm."+table_name+" WHERE domainid in (" + m_domainlist+ ") and name = ?";
				System.out.println("sql="+sql);
			}
		} else {
			if (ot == ObjectType.TEMPLATE) {
				// Templates don't have domains of their own
				sql = "SELECT t.id FROM dm.dm_template t,dm_notify n WHERE t.notifierid=n.id AND t.name = ? AND n.domainid=? AND t.status='N'";
			}
			else
			if (ot == ObjectType.BUILDJOB) {
				// Build Jobs don't have domains of their own
				sql = "SELECT b.id FROM dm.dm_buildengine b,dm_buildjob j WHERE j.builderid=b.id AND j.name = ? AND b.domainid=? AND t.status='N'";
			} else {
				sql = "SELECT id FROM dm."+table_name+" WHERE name = ? AND domainid=?";
			}
			name = name.substring(name.lastIndexOf('.')+1); 
		}
		try
		{
			if (ot != ObjectType.TEMPLATE && ot != ObjectType.TASK) {
				sql = sql + "AND status='N'";
			}
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setString(1,name);
			if (domainid != DOMAIN_NOT_SPECIFIED) stmt.setInt(2, domainid);
			ResultSet rs = stmt.executeQuery();
			DMObject ret = null;
			String d = ot.toString();
			String desc = d.substring(0,1).toUpperCase()+d.substring(1).toLowerCase();
			if(rs.next()) {
				// Check this name is unique
				int id = rs.getInt(1);
				if (rs.next()) {
					// not unique
					rs.close();
					stmt.close();
					throw new RuntimeException(desc+" "+name+" is Ambiguous");
				}
				switch(ot)
				{
				case ENVIRONMENT:	ret = getEnvironment(id,true);break;
				case SERVER:		ret = getServer(id,true);break;
				case USER:			ret = getUser(id);break;
				case DATASOURCE:	ret = getDatasource(id,true);break;
				case COMPONENT:		ret = getComponent(id,true);break;
				case TASK:			ret = getTask(id,true);break;
				case APPLICATION:	ret = getApplication(id,true);break;
				case DOMAIN:		ret = getDomain(id);break;
				case CREDENTIALS:	ret = getCredential(id,true);break;
				case TEMPLATE:		ret = getTemplate(id);break;
				case USERGROUP:		ret = getGroup(id);break;
				default:			break;
				}
			} else {
				// not found
				rs.close();
				stmt.close();
				throw new RuntimeException(desc+" "+name+" Not Found or No Access");
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve object '" + name + "' from database");	
	}
	
	public Environment getEnvironmentByName(String envName)
	{
		return (Environment)getObjectByName(ObjectType.ENVIRONMENT,envName);			
	}
	
	public Server getServerByName(String serverName)
	{	
		return (Server)getObjectByName(ObjectType.SERVER,serverName);	
	}
	
	public User getUserByName(String userName)
	{
		return (User)getObjectByName(ObjectType.USER,userName);	
	}
	
	public Datasource getDatasourceByName(String dsName)
	{	
		return (Datasource)getObjectByName(ObjectType.DATASOURCE,dsName);	
	}
	
	public Component getComponentByName(String componentName)
	{
		return (Component)getObjectByName(ObjectType.COMPONENT,componentName);	
	}
	
	public Task getTaskByName(String taskName)
	{
		Task t = (Task)getObjectByName(ObjectType.TASK,taskName);
		// Now check we have execute access to the task, otherwise throw exception
		String sql = "SELECT count(*) FROM dm.dm_taskaccess a,dm.dm_usersingroup b WHERE a.taskid=? and b.userid=? and a.usrgrpid in (b.groupid,1)";
		try {
			System.out.println("checking execute permissions for task "+t.getId()+" for user "+getUserID());
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,t.getId());
			stmt.setInt(2,getUserID());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				System.out.println("count is "+rs.getInt(1));
				if (rs.getInt(1)==0) throw new RuntimeException("Task "+taskName+" Not Found or No Access");
			}
		} catch(SQLException ex) {
			throw new RuntimeException(ex.getMessage());
		}
		return t;
	}
	
	public Application getApplicationByName(String appName)
	{
		return (Application)getObjectByName(ObjectType.APPLICATION,appName);			
	}
	
	public Domain getDomainByName(String domainName)
	{	
		return (Domain)getObjectByName(ObjectType.DOMAIN,domainName);	
	}
	
	public Credential getCredentialByName(String credName)
	{
		return (Credential)getObjectByName(ObjectType.CREDENTIALS,credName);
	}
	
	public NotifyTemplate getTemplateByName(String tempName)
	{
		return (NotifyTemplate)getObjectByName(ObjectType.TEMPLATE,tempName);
	}
	
	public UserGroup getGroupByName(String groupName)
	{
		return (UserGroup)getObjectByName(ObjectType.USERGROUP,groupName);
	}
	
	public Task getTaskByType(Domain domain,TaskType type)
	{
		Task t = null;
		String tt = type.getTaskTypeString();
		System.out.println("getTaskByType tt="+tt);
		String sql1 = "SELECT id FROM dm.dm_tasktypes WHERE name=?";
		String sql2 = "SELECT id,subdomains FROM dm.dm_task WHERE typeid=? AND domainid=?";
		String sql3 = "SELECT COUNT(*) FROM dm.dm_taskaccess a,dm.dm_usersingroup b WHERE a.taskid=? AND b.userid=? and a.usrgrpid in (b.groupid,1)";
		try {
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
			stmt1.setString(1,tt);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				// Got the task type
				System.out.println("looking for task of type "+rs1.getInt(1)+" in domain "+domain.getId());
				boolean checkInheritence = false;
				stmt2.setInt(1,rs1.getInt(1));
				do {
					stmt2.setInt(2,domain.getId());
					ResultSet rs2 = stmt2.executeQuery();
					while (rs2.next()) {
						// Task of the correct type found in this domain. Do we have access?
						System.out.println("found task "+rs2.getInt(1)+" checking access");
						stmt3.setInt(1,rs2.getInt(1));
						if (!checkInheritence || (checkInheritence && getString(rs2,2,"N").equalsIgnoreCase("y"))) {
							stmt3.setInt(2,getUserID());
							ResultSet rs3 = stmt3.executeQuery();
							if (rs3.next()) {
								if (rs3.getInt(1)>0) {
									System.out.println("** count for task "+rs2.getInt(1)+" is "+rs3.getInt(1));
									// Found a task of the correct type and with execute rights
									t = this.getTask(rs2.getInt(1),false);
								}
							}
							rs3.close();
						}
						if (t != null) break;	// found the first task with execute rights
					}
					rs2.close();
					if (t==null) {
						// Didn't find a task in this domain - go up a domain and check for
						// inherited tasks
						domain = domain.getDomain();
						if (domain == null) break;	// bail out
						checkInheritence = true;
					}
				}
				while (t==null);
			}
			rs1.close();
			stmt1.close();
			if (t != null) System.out.println("found task "+t.getName());
			return t;
		} catch(SQLException ex) {
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve task by type");	
	}
	
	public TableDataSet getPreRequisitiesForApp(Application app)
	{
		String sql1 = "SELECT a.depappid,b.name,a.option,a.notes FROM dm.dm_prerequisities a, dm.dm_application b WHERE a.appid=? AND b.id=a.depappid AND a.deptype in ('APP','AV')";
		String sql2 = "SELECT a.compid,b.name,a.option,a.notes FROM dm.dm_prerequisities a, dm.dm_component b WHERE a.appid=? AND b.id=a.compid AND a.deptype='COMP'";
			
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql1);
			stmt.setInt(1, app.getId());
			ResultSet rs = stmt.executeQuery();
			TableDataSet ret = new TableDataSet(5);
			int row = 0;
			while(rs.next()) {
				ret.put(row, 0, (Integer)rs.getInt(1));
				ret.put(row, 1, "A");
				ret.put(row, 2, new Application(this, rs.getInt(1), rs.getString(2)).getLinkJSON());
				String opt = rs.getString(3);
				if (opt == null) opt="Abort";	// Default
				ret.put(row, 3, opt);
				opt = rs.getString(4);
				if (opt == null) opt="";
				ret.put(row, 4, opt);
				row++;
			}
			rs.close();
			stmt.close();
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			stmt2.setInt(1, app.getId());
			ResultSet rs2 = stmt2.executeQuery();
			while(rs2.next()) {
				ret.put(row, 0, (Integer)rs2.getInt(1));
				ret.put(row, 1, "C");
				ret.put(row, 2, new Component(this, rs2.getInt(1), rs2.getString(2)).getLinkJSON());
				String opt = rs2.getString(3);
				if (opt == null) opt="Abort";	// Default
				ret.put(row, 3, opt);
				opt = rs2.getString(4);
				if (opt == null) opt="";
				ret.put(row, 4, opt);
				row++;
			}
			rs2.close();
			stmt2.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve prereqs for application '" + app.getName() + "' from database");
	}
	
	// For API
	public List<DeployedApplication> getDeployedApplicationsInEnvironment(int envid)
	{
		String sql = "SELECT ai.appid, a1.name, a2.id, a2.name, a2.predecessorid, "
				+ "  vi2.deploymentid, d.exitcode, d.finished, "
				+ "  vi2.modifierid, u.name, u.realname, vi2.modified "
				+ "FROM dm.dm_appsallowedinenv ai "
				+ "LEFT OUTER JOIN dm.dm_application a1 ON a1.id = ai.appid "
				+ "LEFT OUTER JOIN dm.dm_application a2 ON (a2.id = ai.appid OR a2.parentid = ai.appid) AND a2.id IN ("
				+ "  SELECT vi.appid FROM dm.dm_appsinenv vi WHERE vi.envid = ai.envid)"
				+ "LEFT OUTER JOIN dm.dm_appsinenv vi2 ON vi2.envid = ai.envid AND vi2.appid = a2.id "
				+ "LEFT OUTER JOIN dm.dm_deployment d ON d.deploymentid = vi2.deploymentid "
				+ "LEFT OUTER JOIN dm.dm_user u ON u.id = vi2.modifierid "
				+ "WHERE ai.envid = ? ORDER BY 2";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, envid);
			ResultSet rs = stmt.executeQuery();
			List<DeployedApplication> dal = new ArrayList<DeployedApplication>();
			while(rs.next()) {
				DeployedApplication da = new DeployedApplication();
				da.setApplicationID(rs.getInt(1));		// id of application
				da.setApplicationName(rs.getString(2));	// name of application
				da.setVersionID(rs.getInt(3));			// id of application version (NULL if not deployed)
				da.setVersionName(rs.getString(4));		// name of application version (NULL if not deployed)
				da.setPredecessorID(rs.getInt(5));		// predecessor id of application version (NULL if no application version)
				da.setDeploymentID(rs.getInt(6));		// deployment id (NULL if not deployed)
				da.setExitCode(rs.getInt(7));			// exit code (NULL if not deployed)
				// da.setCompletedDate(new Date(rs.getLong(8)*1000));		// Completion Date/Time (NULL if not deployed)
				dal.add(da);
			}
			rs.close();
			stmt.close();
			return dal;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve deployed application version info for environment '" + envid + "' from database");				
	}
	
	public TableDataSet getAppVersInEnvData(Environment env)
	{
		String sql = "SELECT ai.appid, a1.name, a2.id, a2.name, a2.predecessorid, "
			+ "  vi2.deploymentid, d.exitcode, d.finished, "
			+ "  vi2.modifierid, u.name, u.realname, vi2.modified "
			+ "FROM dm.dm_appsallowedinenv ai "
			+ "LEFT OUTER JOIN dm.dm_application a1 ON a1.id = ai.appid "
			+ "LEFT OUTER JOIN dm.dm_application a2 ON (a2.id = ai.appid OR a2.parentid = ai.appid) AND a2.id IN ("
			+ "  SELECT vi.appid FROM dm.dm_appsinenv vi WHERE vi.envid = ai.envid)"
			+ "LEFT OUTER JOIN dm.dm_appsinenv vi2 ON vi2.envid = ai.envid AND vi2.appid = a2.id "
			+ "LEFT OUTER JOIN dm.dm_deployment d ON d.deploymentid = vi2.deploymentid "
			+ "LEFT OUTER JOIN dm.dm_user u ON u.id = vi2.modifierid "
			+ "WHERE ai.envid = ? ORDER BY 2";			
//			+ "WHERE ai.envid = ? AND a1.parentid IS NULL ORDER BY 2";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, env.getId());
			ResultSet rs = stmt.executeQuery();
			TableDataSet ret = new TableDataSet(4);
			int row = 0;
			while(rs.next()) {
				ret.put(row, 0, new JSONBoolean(false));
				ret.put(row, 1, new Application(this, rs.getInt(1), rs.getString(2)).getLinkJSON());
				int avid = getInteger(rs, 3, 0);
				if(avid != 0) {
					Application app = new Application(this, avid, rs.getString(4));
					app.setPredecessorId(getInteger(rs, 5, 0));
					ret.put(row, 2, app.getLinkJSON());
					int deploymentid = getInteger(rs, 6, 0);
					if(deploymentid != 0) {
						Deployment d = new Deployment(this, deploymentid, rs.getInt(7));
						d.setFinished(rs.getInt(8));
						ret.put(row, 3, d.getLinkJSON());
					} else {
						ret.put(row, 3, new CreatedModifiedField(
								formatDateToUserLocale(rs.getInt(12)),
								new User(this, rs.getInt(9), rs.getString(10), rs.getString(11))));						
					}
				}
				row++;
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve application version info for environment '" + env.getName() + "' from database");				
	}
	
	public TableDataSet envListToTableData(List<Environment> envs)
	{
		Object envlist[] = envs.toArray();
		TableDataSet ret = new TableDataSet(4);
		for (int i=0;i<envs.size();i++) {
			Environment env = (Environment)envlist[i];
			ret.put(i, 0, new JSONBoolean(false));
			ret.put(i, 1, env.getLinkJSON());
		}
		return ret;
	}
	
	public TableDataSet getAppVersInEnvData(Application app)
	{
		String sql;
		int parentid = app.getParentId();
		if (parentid>0) {
			// this is an application version
			sql = "SELECT ai.envid, e.name, a2.id, a2.name, a2.predecessorid, "
				+ "  vi2.deploymentid, d.exitcode, d.finished, "
				+ "  vi2.modifierid, u.name, u.realname, vi2.modified "
				+ "FROM dm.dm_appsallowedinenv ai "
				+ "LEFT OUTER JOIN dm.dm_environment e ON e.id = ai.envid "
				+ "LEFT OUTER JOIN dm.dm_application a2 ON (a2.id = ai.appid OR a2.parentid = ai.appid) AND a2.id IN ("
				+ "  SELECT vi.appid FROM dm.dm_appsinenv vi WHERE vi.envid = ai.envid)"
				+ "LEFT OUTER JOIN dm.dm_appsinenv vi2 ON vi2.envid = ai.envid AND vi2.appid = a2.id "
				+ "LEFT OUTER JOIN dm.dm_deployment d ON d.deploymentid = vi2.deploymentid "
				+ "LEFT OUTER JOIN dm.dm_user u ON u.id = vi2.modifierid "
				+ "WHERE ai.appid = ? AND a2.id = ? ORDER BY 2";
		} else {
			// this is an application
			sql = "SELECT ai.envid, e.name, a2.id, a2.name, a2.predecessorid, "
				+ "  vi2.deploymentid, d.exitcode, d.finished, "
				+ "  vi2.modifierid, u.name, u.realname, vi2.modified "
				+ "FROM dm.dm_appsallowedinenv ai "
				+ "LEFT OUTER JOIN dm.dm_environment e ON e.id = ai.envid "
				+ "LEFT OUTER JOIN dm.dm_application a2 ON (a2.id = ai.appid OR a2.parentid = ai.appid) AND a2.id IN ("
				+ "  SELECT vi.appid FROM dm.dm_appsinenv vi WHERE vi.envid = ai.envid)"
				+ "LEFT OUTER JOIN dm.dm_appsinenv vi2 ON vi2.envid = ai.envid AND vi2.appid = a2.id "
				+ "LEFT OUTER JOIN dm.dm_deployment d ON d.deploymentid = vi2.deploymentid "
				+ "LEFT OUTER JOIN dm.dm_user u ON u.id = vi2.modifierid "
				+ "WHERE ai.appid = ? ORDER BY 2";
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, parentid>0?parentid:app.getId());
			if (parentid>0) stmt.setInt(2,app.getId());
			ResultSet rs = stmt.executeQuery();
			TableDataSet ret = new TableDataSet(4);
			int row = 0;
			while(rs.next()) {
				ret.put(row, 0, new JSONBoolean(false));
				ret.put(row, 1, new Environment(this, rs.getInt(1), rs.getString(2)).getLinkJSON());
				int avid = getInteger(rs, 3, 0);
				if(avid != 0) {
					Application av = new Application(this, avid, rs.getString(4));
					av.setPredecessorId(getInteger(rs, 5, 0));
					ret.put(row, 2, av.getLinkJSON());
					int deploymentid = getInteger(rs, 6, 0);
					if(deploymentid != 0) {
						Deployment d = new Deployment(this, deploymentid, rs.getInt(7));
						d.setFinished(rs.getInt(8));
						ret.put(row, 3, d.getLinkJSON());
					} else {
						ret.put(row, 3, new CreatedModifiedField(
								formatDateToUserLocale(rs.getInt(12)),
								new User(this, rs.getInt(9), rs.getString(10), rs.getString(11))));						
					}
				}
				row++;
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve application version info for environment '" + app.getName() + "' from database");				
	}
	
	public TableDataSet getPendingEnvData(Environment env)
	{
		long cutoff = (System.currentTimeMillis() / 1000) - 864000;	// 10 days ago
		String sql = "SELECT c.id,c.name,c.parentid,b.id,b.name,d.id,d.name,a.starttime,a.endtime,a.eventname,a.id	"
				+	"FROM 	dm.dm_calendar a, 		"
				+	"		dm.dm_environment 	b,	"
				+	"		dm.dm_application	c,	"
				+	"		dm.dm_domain		d	"
				+	"WHERE	a.envid = b.id			"
				+	"AND	a.appid = c.id			"
				+	"AND	b.domainid = d.id		"
				+	"AND	a.envid=?				"
				+	"AND	a.status <> 'D' 		"
				+	"AND	a.endtime > ?			"
				+	"ORDER BY c.name";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,env.getId());
			stmt.setLong(2,cutoff);
			ResultSet rs = stmt.executeQuery();
			TableDataSet ret = new TableDataSet(8);
			int row = 0;
			while(rs.next()) {
				ret.put(row, 0, rs.getInt(8));
				ret.put(row, 1, rs.getInt(9));
				Application app = new Application(this, rs.getInt(1), rs.getString(2));
				app.setParentId(getInteger(rs,3,0));
				System.out.println("id="+app.getId()+" parentid="+app.getParentId());
				ret.put(row, 2, app.getLinkJSON());
				ret.put(row, 3, rs.getInt(6));
				ret.put(row, 4, rs.getString(7));
				ret.put(row, 5, new Environment(this, rs.getInt(4), rs.getString(5)).getLinkJSON());
				ret.put(row, 6, rs.getString(10));
				ret.put(row, 7, rs.getInt(11));
				row++;
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve pending application info for environment '" + env.getName() + "' from database");				
	}
	
	public TableDataSet getPendingEnvData(Application app)
	{
		System.out.println("getPendingEnvData for app "+app.getId());
		String col = (app.getParentId()>0)?"c.id":"c.parentid";
		String sql;
		boolean isRelease = (app.getIsRelease().equalsIgnoreCase("y"));
		System.out.println("isRelease="+isRelease);
		if (isRelease) {
			sql = 	"SELECT x.id,x.name,x.parentid,0,'',0,'',0 starttime,0,'',0,0,'Y'	"
					+ "FROM dm.dm_application x	"
					+ "WHERE x.id=?	"
					+ "AND NOT EXISTS (	"
					+ "SELECT 'Y' FROM dm.dm_calendar t WHERE t.appid=?	"
					+ ")	"
					+ "UNION	"
					+ "SELECT c.id,c.name,c.parentid,b.id,b.name,d.id,d.name,a.starttime,a.endtime,a.description,a.id,d.position,c.isrelease	"
					+	"FROM 	dm.dm_calendar 		a,	"
					+	"		dm.dm_environment 	b,	"
					+	"		dm.dm_application	c,	"
					+	"		dm.dm_domain		d	"
					+	"WHERE	a.envid = b.id			"
					+	"AND	a.appid = c.id			"
					+	"AND	b.domainid = d.id		"
					+	"AND	a.status <> 'D'			"
					+	"AND	c.status <> 'D'			"
					+	"AND	c.id IN	"
					+	"(SELECT objfrom FROM dm.dm_applicationcomponentflows x WHERE x.appid=? "
					+	"UNION "
					+	"SELECT objto FROM dm.dm_applicationcomponentflows y WHERE y.appid=?) "
					+	"UNION "
					+	"SELECT c.id,c.name,c.parentid,b.id,b.name,d.id,d.name,a.starttime,a.endtime,a.description,a.id,d.position,c.isrelease	"
					+	"FROM 	dm.dm_calendar a, 		"
					+	"		dm.dm_environment 	b,	"
					+	"		dm.dm_application	c,	"
					+	"		dm.dm_domain		d	"
					+	"WHERE	a.envid = b.id			"
					+	"AND	a.appid = c.id			"
					+	"AND	b.domainid = d.id		"
					+	"AND	("+col+"=? or c.id=?)	"
					+	"AND	a.status <> 'D'			"
					+	"AND	c.status <> 'D'			"
					+	"UNION	"
					+	"SELECT x2.id,x2.name,x2.parentid,0,'',0,'',0,0,'',x2.id,0,'N'	"
					+	"FROM	dm.dm_application x2	"
					+	"WHERE NOT EXISTS (SELECT 'Y' FROM dm.dm_calendar z WHERE z.appid=x2.id)	"
					+	"AND	x2.id IN	"
					+	"(SELECT objfrom FROM dm.dm_applicationcomponentflows x1 WHERE x1.appid=?	"
					+	"UNION	"
					+	"SELECT objto FROM dm.dm_applicationcomponentflows y1 WHERE y1.appid=?)	"
					+	"ORDER BY 13 desc,starttime";
		} else {
			sql = "SELECT c.id,c.name,c.parentid,b.id,b.name,d.id,d.name,a.starttime,a.endtime,a.description,a.id,d.position,c.isrelease	"
				+	"FROM 	dm.dm_calendar a, 		"
				+	"		dm.dm_environment 	b,	"
				+	"		dm.dm_application	c,	"
				+	"		dm.dm_domain		d	"
				+	"WHERE	a.envid = b.id			"
				+	"AND	a.appid = c.id			"
				+	"AND	b.domainid = d.id		"
				+	"AND	("+col+"=? or c.id=?)	"
				+	"AND	a.status <> 'D'			"
				+	"AND	c.status <> 'D'		"
				+	"UNION	"
				+	"SELECT c.id,c.name,c.parentid,b.id,b.name,d.id,d.name,a.starttime,a.endtime,a.description,a.id,d.position,c.isrelease  " 
				+	"FROM    dm.dm_calendar a, "
				+	"        dm.dm_environment       		b, "    
				+	"        dm.dm_application      		c, "
				+	"        dm.dm_domain            		d "
				+	"WHERE   a.envid = b.id "          
				+	"AND     a.appid = c.id "           
				+	"AND     b.domainid = d.id "            
				+	"AND     c.isrelease='Y' "
				+	"AND	 c.id IN ( "
				+	"SELECT appid FROM dm.dm_applicationcomponentflows WHERE (objfrom IN ( "
				+	"SELECT id FROM dm.dm_application WHERE (id=? or parentid=?) and status<>'D' "
				+	") OR objto IN (select id from dm.dm_application where (id=? or parentid=?) and status<>'D') "
				+	")) "
				+	"AND     a.status <> 'D'  "               
				+	"AND     c.status <> 'D' "
				+	"ORDER BY starttime";
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,app.getId());
			stmt.setInt(2,app.getId());
			stmt.setInt(3,app.getId());
			stmt.setInt(4,app.getId());
			stmt.setInt(5,app.getId());
			stmt.setInt(6,app.getId());
			if (isRelease) {			
				stmt.setInt(7,app.getId());
				stmt.setInt(8,app.getId());
			}
			ResultSet rs = stmt.executeQuery();
			TableDataSet ret = new TableDataSet(12);
			int row = 0;
			long now = timeNow();
			System.out.println("timeNow="+now);
			String csql = "SELECT count(*) FROM dm.dm_appsinenv WHERE envid=? AND appid=?";
			PreparedStatement cstmt = getDBConnection().prepareStatement(csql);
			while(rs.next()) {	
				System.out.println(row+") got application "+rs.getString(2)+" (id "+rs.getInt(1)+")");
				ret.put(row, 0, rs.getInt(8));		// start time
				ret.put(row, 1, rs.getInt(9));		// end time
				Application a = new Application(this, rs.getInt(1), rs.getString(2));
				a.setParentId(getInteger(rs,3,0));
				ret.put(row, 2, a.getLinkJSON());	// Application
				ret.put(row, 3, rs.getInt(6));		// Domain ID
				ret.put(row, 4, rs.getString(7));	// Domain Name
				ret.put(row, 5, new Environment(this, rs.getInt(4), rs.getString(5)).getLinkJSON());
				ret.put(row, 6, rs.getString(10));	// Description
				ret.put(row, 7, rs.getInt(11));		// Calendar Event ID
				ret.put(row, 8, rs.getInt(1));		// Application ID (for release grouping)
				ret.put(row, 9, rs.getString(2));	// Application Name (for release grouping)
				ret.put(row, 10, rs.getInt(12));	// Domain Order (for sub grouping releases)
				String status="";
				boolean rel = rs.getString(13).equalsIgnoreCase("y");
				if (now > rs.getInt(8)) {
					System.out.println("time has passed for app "+rs.getString(2)+" "+now+" > "+rs.getInt(8));
					// Start time has passed - check the app is present on the environment
					cstmt.setInt(1,rs.getInt(4));	// envid
					cstmt.setInt(2,rs.getInt(1));	// appid
					ResultSet crs = cstmt.executeQuery();
					if (crs.next()) {
						int cnt = crs.getInt(1);
						if (cnt > 0) {
							// Application is present in Environment - was it late?
							status = "inenv";
						} else {
							// Application is not present in Environment
							status = "absent";
						}
					}
				} else {
					status=rel?"release":"";
				}
				System.out.println("putting row for appid "+rs.getInt(1));
				ret.put(row, 11, status);			
				row++;
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve pending environment info for application '" + app.getName() + "' from database");				
	}
	
	
	public boolean addToPrerequisities(Application app, Component comp)
	{
		int parentid = comp.getParentId();
		System.out.println("addToPrerequisities - " + app.getId() + ", " + comp.getId());
		String sql = "INSERT INTO dm.dm_prerequisities(appid, deptype, compid) VALUES(?,'COMP',?)";
		try {
			boolean ret=false;;
			String updsql = "UPDATE dm.dm_prerequisities SET compid=? WHERE appid=? AND (compid IN (SELECT id FROM dm.dm_component WHERE parentid=?) OR compid=?)";
			PreparedStatement updstmt = getDBConnection().prepareStatement(updsql);
			updstmt.setInt(1,comp.getId());
			updstmt.setInt(2,app.getId());
			updstmt.setInt(3,parentid>0?parentid:comp.getId());
			updstmt.setInt(4,parentid);
			updstmt.execute();
			int updcount = updstmt.getUpdateCount();
			updstmt.close();
			System.out.println("upcount="+updcount);
			if (updcount == 0) {
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1, app.getId());
				stmt.setInt(2, comp.getId());
				stmt.execute();
				System.out.println(stmt.getUpdateCount() + " rows inserted");
				ret = (stmt.getUpdateCount() == 1);
				stmt.close();
			} else {
				ret = true;
			}
			getDBConnection().commit();	
			return ret;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean addToPrerequisities(Application app, Application depapp)
	{
		int parentid = depapp.getParentId();
		
		System.out.println("addToPrerequisities - " + app.getId() + ", " + depapp.getId());
		String sql = "INSERT INTO dm.dm_prerequisities(appid, deptype, depappid) VALUES(?,?,?)";
		try {
			//
			//
			boolean ret=false;;
			String updsql = "UPDATE dm.dm_prerequisities SET depappid=? WHERE appid=? AND (depappid IN (SELECT id FROM dm.dm_application WHERE parentid=?) OR depappid=?)";
			PreparedStatement updstmt = getDBConnection().prepareStatement(updsql);
			updstmt.setInt(1,depapp.getId());
			updstmt.setInt(2,app.getId());
			updstmt.setInt(3,parentid>0?parentid:depapp.getId());
			updstmt.setInt(4,parentid);
			updstmt.execute();
			int updcount = updstmt.getUpdateCount();
			updstmt.close();
			if (updcount == 0) {
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1, app.getId());
				stmt.setString(2, parentid > 0 ? "AV":"APP");
				stmt.setInt(3, depapp.getId());
				stmt.execute();
				System.out.println(stmt.getUpdateCount() + " rows inserted");
				ret = (stmt.getUpdateCount() == 1);
				stmt.close();
			} else {
				ret = true;
			}
			getDBConnection().commit();
			return ret;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean ChangePrerequisite(Application app, ObjectType t, int id, String coltype, String colval)
	{
		System.out.println("app.id="+app.getId()+" t="+t+" id="+id+" coltype="+coltype+" colval="+colval);
		String keycol = (t==ObjectType.COMPONENT)?"compid":"depappid";
		String colname = (coltype.equalsIgnoreCase("action"))?"option":"notes";
		String sql = "UPDATE dm.dm_prerequisities SET "+colname+"=? WHERE appid=? AND "+keycol+"=?";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setString(1, colval);
			stmt.setInt(2, app.getId());
			stmt.setInt(3,id);
			stmt.execute();
			System.out.println(stmt.getUpdateCount() + " rows updated");
			boolean ret = (stmt.getUpdateCount() == 1);
			getDBConnection().commit();
			stmt.close();
			return ret;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	public boolean addToAppsAllowedInEnv(Environment env, Application app)
	{
		System.out.println("addToAppsAllowedInEnv - " + env.getId() + ", " + app.getId());
		int pappid = app.getParentId();
		String sql1 = "SELECT count(*) FROM dm.dm_appsallowedinenv WHERE envid=? AND appid=?";
		String sql2 = "INSERT INTO dm.dm_appsallowedinenv(envid, appid) VALUES(?,?)";
		boolean ret=false;
		try {
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			stmt1.setInt(1, env.getId());
			stmt1.setInt(2, pappid>0?pappid:app.getId());
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				int c = rs1.getInt(1);
				if (c == 0) {
					stmt2.setInt(1, env.getId());
					stmt2.setInt(2, pappid>0?pappid:app.getId());
					stmt2.execute();
					System.out.println(stmt2.getUpdateCount() + " rows inserted");
					ret = (stmt2.getUpdateCount() == 1);
					if (ret == true) {
						// Row added - record the fact for both the app and env
						String pf=(pappid>0)?"av":"ap";
						String linkval="<a href='javascript:SwitchDisplay(\""+pf+app.getId()+"\");'><b>"+app.getName()+"</b></a>";
						String linkval2="<a href='javascript:SwitchDisplay(\"en"+env.getId()+"\");'><b>"+env.getName()+"</b></a>";
						RecordObjectUpdate(app,"Associated with Environment "+linkval2,env.getId());
						RecordObjectUpdate(env,"Application "+linkval+" Associated",app.getId());
					}
					getDBConnection().commit();
					stmt2.close();
				} else {
					ret=true;
				}
			}
			return ret;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	public boolean removeFromAppsAllowedInEnv(Environment env, Application app)
	{
		System.out.println("removeFromAppsAllowedInEnv - " + env.getId() + ", " + app.getId());
		int pappid = app.getParentId();
		String sql1 = "DELETE FROM dm.dm_appsallowedinenv WHERE envid = ? AND appid = ?";
		String sql2 = "DELETE FROM dm.dm_appsinenv WHERE envid = ? AND appid IN "
				+ "(select id FROM dm.dm_application WHERE parentid=?)";
		try {
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1, env.getId());
			stmt1.setInt(2, pappid>0?pappid:app.getId());
			stmt1.execute();
			System.out.println(stmt1.getUpdateCount() + " rows deleted");
			boolean ret1 = (stmt1.getUpdateCount() == 1);
			//
			// Now remove any entry in appsinenv
			//
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			stmt2.setInt(1, env.getId());
			stmt2.setInt(2, pappid>0?pappid:app.getId());
			stmt2.execute();
			System.out.println(stmt2.getUpdateCount() + " rows deleted");
			String pf=(pappid>0)?"av":"ap";
			String linkval="<a href='javascript:SwitchDisplay(\""+pf+app.getId()+"\");'><b>"+app.getName()+"</b></a>";
			String linkval2="<a href='javascript:SwitchDisplay(\"en"+env.getId()+"\");'><b>"+env.getName()+"</b></a>";
			RecordObjectUpdate(app,"Disassociated from Environment "+linkval2,env.getId());
			RecordObjectUpdate(env,"Application "+linkval+" Disassociated",app.getId());
			getDBConnection().commit();
			stmt2.close();
			return ret1;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	public boolean setAppInEnv(Environment env, Application app, String note)
	{
		System.out.println("in setAppInEnv");
		System.out.println("setAppInEnv - " + env.getId() + ", " + app.getId() + ", " + note);
		String usql = "UPDATE dm.dm_appsinenv SET deploymentid = null, appid = ?, modifierid = ?, modified = ?, note = ? "
			+ "WHERE envid = ? AND appid IN (SELECT a.id FROM dm.dm_application a, dm.dm_application b "
			+ "WHERE (a.parentid = b.parentid OR a.id = b.parentid OR a.parentid = b.id OR a.id = b.id) AND b.id = ?)";
		String isql = "INSERT INTO dm.dm_appsinenv(envid, appid, modifierid, modified, note) VALUES(?,?,?,?,?)";
		String msql = "SELECT coalesce(min(deploymentid),-1) FROM dm.dm_deployment WHERE deploymentid < 0";
		String isql2 = "insert into dm.dm_deployment(deploymentid,userid,startts,appid,envid,started) values(?,?,now(),?,?,?)";
		try {
			long t = timeNow();
			PreparedStatement stmt = getDBConnection().prepareStatement(usql);
			stmt.setInt(1, app.getId());
			stmt.setInt(2, getUserID());
			stmt.setLong(3, t);
			stmt.setString(4, note);
			stmt.setInt(5, env.getId());
			stmt.setInt(6, app.getId());
			stmt.execute();
			System.out.println(stmt.getUpdateCount() + " rows updated");
			if(stmt.getUpdateCount()==0) {
				PreparedStatement stmt2 = getDBConnection().prepareStatement(isql);
				stmt2.setInt(1, env.getId());
				stmt2.setInt(2, app.getId());
				stmt2.setInt(3, getUserID());
				stmt2.setLong(4, t);
				stmt2.setString(5, note);
				stmt2.execute();
				System.out.println(stmt.getUpdateCount() + " rows inserted");
				stmt2.close();
			}
			// insert a "dummy" deployment row to indicate manual update
			Statement mstmt = getDBConnection().createStatement();
			ResultSet rsm = mstmt.executeQuery(msql);
			if (rsm.next()) {
				PreparedStatement dstmt = getDBConnection().prepareStatement(isql2);
				dstmt.setInt(1,rsm.getInt(1)-1);
				dstmt.setInt(2,getUserID());
				dstmt.setInt(3,app.getId());
				dstmt.setInt(4,env.getId());
				dstmt.setLong(5,t);
				dstmt.execute();
				System.out.println(dstmt.getUpdateCount() + " rows inserted");
				dstmt.close();
			}
			mstmt.close();
			stmt.close();		
			getDBConnection().commit();		
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	public boolean clearAppInEnv(Environment env, Application app)
	{
		System.out.println("clearAppInEnv - " + env.getId() + ", " + app.getId());
		String usql = "DELETE FROM dm.dm_appsinenv WHERE envid = ? AND appid IN "
			+ "(SELECT a.id FROM dm.dm_application a, dm.dm_application b "
			+ "WHERE (a.parentid = b.parentid OR a.id = b.parentid OR a.parentid = b.id OR a.id = b.id) AND b.id = ?)";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(usql);
			stmt.setInt(1, env.getId());
			stmt.setInt(2, app.getId());
			stmt.execute();
			System.out.println(stmt.getUpdateCount() + " rows updated");
			boolean ret = (stmt.getUpdateCount()==1);
			getDBConnection().commit();
			stmt.close();
			return ret;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}

	
	public ReportDataSet getDeploymentsPerUserForEnvironment(int envid)
	{
		String sql = "SELECT u.name, ("
			+ "SELECT count(*) FROM dm.dm_deployment d WHERE d.userid=u.id AND d.envid=? AND d.exitcode = 0"
			+ ") as success, ("
			+ "SELECT count(*) FROM dm.dm_deployment d WHERE d.userid=u.id AND d.envid=? AND d.exitcode <> 0"
			+ ") as failed "
			+ "FROM dm.dm_user u ORDER BY u.name";
		try
		{	
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, envid);
			stmt.setInt(2, envid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve user deployments report from database");		
	}
	
	
	public ReportDataSet getDeploymentsPerApplicationForEnvironment(int envid)
	{
		String sql = "SELECT a.name, ("
			+ "SELECT count(*) FROM dm.dm_deployment d WHERE d.appid = a.id AND d.envid=? AND d.exitcode = 0"
			+ ") as success, ("
			+ "SELECT count(*) FROM dm.dm_deployment d WHERE d.appid = a.id AND d.envid=? AND d.exitcode <> 0"
			+ ") as failed "
			+ "FROM dm.dm_application a, dm.dm_appsallowedinenv aie WHERE a.id = aie.appid AND aie.envid=? "
			+ "ORDER BY a.name";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, envid);
			stmt.setInt(2, envid);
			stmt.setInt(3, envid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve application deployments report from database");		
	}
	
	// Application
	
	private String getParentLabel(int t,int appid)
	{
		String tablename=(t==0)?"dm_application":"dm_component";
		try
		{
			if (appid>0) {
				PreparedStatement stmt = getDBConnection().prepareStatement("SELECT a.branch,a.predecessorid FROM dm."+tablename+" a WHERE a.id = ?");
				stmt.setInt(1, appid);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					String BranchLabel = getString(rs,1,"");
					int parent = getInteger(rs,2,0);
					rs.close();
					stmt.close();
					if (BranchLabel.length()>0) {
						return BranchLabel;
					} else {
						return getParentLabel(t,parent);
					}
				}
				return "";
			} else {
				return "";
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to get parentlabel for application " + appid + " from database");		
	}
	
	/*
	public String getBranchName(int appid)
	{
		// Returns the branch name for the specified application. If the app has no branch the code
		// checks the parent and so on up the tree until it finds a branch id.
		try {
			String branchname = null;
			int predecessorid=0;
			PreparedStatement stmt = m_conn.prepareStatement("select branch,predecessorid from dm_application where id= ?");
			stmt.setInt(1, appid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				branchname = rs.getString(1);
				if (rs.wasNull()) branchname = null;
				predecessorid = rs.getInt(2);
				if (rs.wasNull()) predecessorid = 0;
			}
			rs.close();
			return (branchname != null)?branchname:(predecessorid>0)?getBranchName(predecessorid):null;
			
		} catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve application " + appid + " from database");		
	}
	*/
	

	
	public Application getApplication(int appid, boolean detailed)
	{
		if (appid < 0) {
			Application ret = new Application(this, appid, "");
			ret.setName("");
			ret.setSummary("");
			ret.setPreAction(new Action(this, 0, ""));
			ret.setPostAction(new Action(this, 0, ""));
			ret.setCustomAction(new Action(this, 0, ""));

			ret.setSuccessTemplate(new NotifyTemplate(this,0,""));
			ret.setFailureTemplate(new NotifyTemplate(this,0,""));
			return ret;
		}
		String sql = null;
		if(detailed) {
			sql = "SELECT a.name, a.summary, a.predecessorid, a.parentid, a.domainid, a.branch, a.status, a.isrelease, a.datasourceid, "
				+ "  uc.id, uc.name, uc.realname, a.created, " // 10 - 13
				+ "  um.id, um.name, um.realname, a.modified, "	// 14 - 17
				+ "  uo.id, uo.name, uo.realname, g.id, g.name, " // 18 - 22
				+ "  a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid, a3.id, a3.name, a3.domainid, " // 23 -31
				+ "  a.xpos, a.ypos, a.startx, b.domainid, " // 32 - 35
				+ "	 t1.id,t2.id " // 36 - 37
				+ "FROM dm.dm_application a "
				+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "				// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "				// modifier
				+ "LEFT OUTER JOIN dm.dm_user uo ON a.ownerid = uo.id "					// owner user
				+ "LEFT OUTER JOIN dm.dm_usergroup g ON a.ogrpid = g.id "				// owner group
				+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "			// pre-action
				+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "			// post-action
				+ "LEFT OUTER JOIN dm.dm_action a3 ON a.actionid = a3.id "				// custom action
				+ "LEFT OUTER JOIN dm.dm_application b ON a.parentid = b.id "			// base application (if this is a version)
				+ "LEFT OUTER JOIN dm.dm_template t1 ON a.successtemplateid = t1.id "	// success notification template
				+ "LEFT OUTER JOIN dm.dm_template t2 ON a.failuretemplateid = t2.id "	// failure notification template
				+ "WHERE a.id = ?";	
		} else {
			sql = "SELECT a.name, a.summary, a.predecessorid, a.parentid, a.domainid, a.branch, a.status, a.isrelease, a.datasourceid  "
				+ "FROM dm.dm_application a WHERE a.id = ?";	
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, appid);
			ResultSet rs = stmt.executeQuery();
			Application ret = null;
			if(rs.next()) {
				ret = new Application(this, appid, rs.getString(1));
				ret.setSummary(rs.getString(2));
				ret.setPredecessorId(getInteger(rs,3,0));
				ret.setParentId(getInteger(rs,4,0));
				int domainId = getInteger(rs,5,0);
				ret.setDomainId(domainId);
				String thislabel = getString(rs,6,"");
				ret.setLabel(thislabel);
				if (thislabel.length() == 0) {
					// No label for this version
					String ParentLabel = getParentLabel(0,getInteger(rs,3,0));
					ret.setParentLabel(ParentLabel);
				}
				getStatus(rs, 7, ret);
				ret.setIsRelease(getString(rs, 8, "N"));
				int dsid = getInteger(rs,9,0);
				if (dsid>0) {
					Datasource ds = this.getDatasource(dsid,true);
					ret.setDatasource(ds);
				}
				if(detailed) {
					getCreatorModifierOwner(rs, 10, ret);
					getPreAndPostActions(rs, 23, ret);		  
					int custactionid = getInteger(rs, 29, 0);
					if (custactionid != 0) {
						ret.setCustomAction(new Action(this, custactionid, rs.getString(30), rs.getInt(31)));
					}
					ret.setXpos(getInteger(rs, 32, 0));
					ret.setYpos(getInteger(rs, 33, 0));
					ret.setStartX(getInteger(rs, 34, 300));	// 300 is default start x position (start window for components)
					ret.setBaseDomain(getInteger(rs, 35, domainId));	// domainid of parent (if version) or this domain (if base)
					int stid = getInteger(rs,36,0);
					int ftid = getInteger(rs,37,0);
					NotifyTemplate st = stid>0?getTemplate(stid):null;
					NotifyTemplate ft = ftid>0?getTemplate(ftid):null;
					ret.setSuccessTemplate(st);
					ret.setFailureTemplate(ft);
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve application " + appid + " from database");				
	}
	
	public Application getLatestVersion(Application app,String branch)
	{
		try {
			int lvid=0;
			if (branch != null) {
				// Branch id specified
				String sql = "select max(id) from dm_application where parentid=? and branch=? and status<>'D'";
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1,app.getId());
				stmt.setString(2, branch);
				ResultSet rs = stmt.executeQuery();
				rs.next();
				int id = rs.getInt(1);
				if (rs.wasNull()) id=0;
				rs.close();
				while (id > 0) {
					lvid=id;
					// Descend this branch until there's no more versions
					String sql2 = "select max(id) from dm_application where predecessorid=? and status<>'D'";
					PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
					stmt2.setInt(1, id);
					ResultSet rs2 = stmt2.executeQuery();
					rs2.next();
					id = rs2.getInt(1);
					if (rs2.wasNull()) id=0;
					rs2.close();
				}
			} else {
				// No branch - just get latest version
				String sql="select max(id) from dm_application where parentid=? and status<>'D'";
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1,app.getId());
				ResultSet rs = stmt.executeQuery();
				rs.next();
				lvid = rs.getInt(1);
				if (rs.wasNull()) lvid=app.getId();	// no children so "latest" is this version
				rs.close();
			}
			return (lvid>0)?getApplication(lvid,true):null;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve latest version of application " + app.getName() + " from database");				
	}
	
	public String getApprovalStatus(int appid)
	{
		// Returns "Y" if the specified application is approved for domains later in the lifecycle
		// than its current lifecycle position, "N" if it is rejected, null if there is no record. Used
		// by lifecycle summary page to indicate approved/rejected applications at each stage.
		//
		String sql = 	"SELECT a.approved FROM dm_approval a,dm_application x,dm_domain b, dm_domain c "
				+		"WHERE a.appid=? AND x.id=a.appid AND b.id = x.domainid AND c.id = a.domainid AND "
				+		"b.domainid = c.domainid AND c.position > b.position ORDER BY a.id DESC";
		String ret=null;
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,appid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String approved = rs.getString(1);
				System.out.println("Approved is "+approved);
				if (approved.equalsIgnoreCase("y")) {
					ret="Y";
				} else {
					ret="N";
				}
			}
			rs.close();
			stmt.close();
			System.out.println("returning "+ret);
			return ret;
		} catch(SQLException ex) {
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Failed to get approval status for application "+appid);
	}
	
	public List<Domain> getApprovalDomains(int appid)
	{
		System.out.println("getApprovalDomains("+appid+")");
		List<Domain> ret = new ArrayList<Domain>();
		try {
			String sql = "SELECT approved,domainid FROM dm_approval WHERE appid=? ORDER BY id DESC";
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,appid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String approved = rs.getString(1);
				System.out.println("approved is "+approved);
				if (approved.equalsIgnoreCase("y")) {
					Domain domain = getDomain(rs.getInt(2));
					ret.add(domain);
				}
			}
			rs.close();
			stmt.close();
			System.out.println("returning size "+ret.size());
			return ret;
		} catch(SQLException ex) {
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Failed to get approval status for application "+appid);
	}
	
	public Component getLatestVersion(Component comp,String branch)
	{
		try {
			int lvid=0;
			if (branch != null) {
				// Branch id specified
				String sql = "select max(id) from dm_component where parentid=? and branch=? and status='N'";
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1,comp.getId());
				stmt.setString(2, branch);
				ResultSet rs = stmt.executeQuery();
				rs.next();
				int id = rs.getInt(1);
				if (rs.wasNull()) id=0;
				rs.close();
				while (id > 0) {
					lvid=id;
					// Descend this branch until there's no more versions
					String sql2 = "select max(id) from dm_component where predecessorid=? and branch is null and status='N'";
					PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
					stmt2.setInt(1, id);
					ResultSet rs2 = stmt2.executeQuery();
					rs2.next();
					id = rs2.getInt(1);
					if (rs2.wasNull()) id=0;
					rs2.close();
				}
			} else {
				// No branch - just get latest version
				String sql="select max(id) from dm_component where parentid=? and status='N'";
				PreparedStatement stmt = getDBConnection().prepareStatement(sql);
				stmt.setInt(1,comp.getId());
				ResultSet rs = stmt.executeQuery();
				rs.next();
				lvid = rs.getInt(1);
				if (rs.wasNull()) lvid=comp.getId(); // If no version, return original base version.
				rs.close();
			}
			return (lvid>0)?getComponent(lvid,true):null;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve latest version of component " + comp.getName() + " from database");				
	}
	
	public JSONObject applicationApprovalDomains(Application app)
	{
		JSONObject ret = new JSONObject();
		JSONArray a1 = new JSONArray();	// approvals
		JSONArray a2 = new JSONArray();	// rejections
		try {
			String sql 	= "select a.approved, a.domainid, d.name, u.name, a.note, a."+whencol+" from dm_approval a, dm_domain d, dm_user u "
						+ "where a.id in (select max(b.id) from dm_approval b "
						+ "where b.appid = ? AND b.domainid = a.domainid) and d.id = a.domainid and u.id = a.userid";
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,app.getId());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject d = new JSONObject();
				d.add("domain", rs.getString(3));
				d.add("user",rs.getString(4));
				d.add("note",rs.getString(5));
				d.add("timestamp",rs.getInt(6));
				String status = rs.getString(1);
				if (status != null && status.equalsIgnoreCase("Y")) {
					a1.add(d);
				} else {
					a2.add(d);
				}
			}
			rs.close();
			ret.add("approvals", a1);
			ret.add("rejections", a2);
			return ret;
			
		} catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve approvals for application " + app.getName() + " from database");				
	}
	
	
	
	
	private boolean updateObjectSummaryField(Object obj, DynamicQueryBuilder update, SummaryField field, SummaryChangeSet changes)
	{
		switch(field) {
		case NAME: {
			String name = (String) changes.get(field);
			if (name.replaceAll("[A-Za-z0-9_ ]","").length()>0) {
				throw new RuntimeException("Invalid Object Name"); 
			}
			if ((name != null) && (name.length() > 0)) {
				if (obj instanceof Domain) {
					Domain dom = (Domain)obj;
					if (CheckIfObjectExists("domain",dom.getId(),name)) {
						throw new RuntimeException("A Domain with that name already exists in the same domain"); 
					}
				} else
				if (obj instanceof User) {
					User user = (User)obj;
					if (CheckIfObjectExists("user",user.getId(),name)) {
						throw new RuntimeException("A User with that name already exists in the same domain"); 
					}
				} else
				if (obj instanceof UserGroup) {
					UserGroup group = (UserGroup)obj;
					if (CheckIfObjectExists("usergroup",group.getId(),name)) {
						throw new RuntimeException("A User Group with that name already exists in the same domain"); 
					}
				} else
				if (obj instanceof Environment) {
					Environment env = (Environment)obj;
					if (CheckIfObjectExists("environment",env.getId(),name)) {
						throw new RuntimeException("An Environment with that name already exists in the same domain"); 
					}
				} else
				if (obj instanceof Server) {
					Server serv = (Server)obj;
					if (CheckIfObjectExists("server",serv.getId(),name)) {
						throw new RuntimeException("A Server with that name already exists in the same domain"); 
					}
				} else
				if (obj instanceof Repository) {
					Repository rep = (Repository)obj;
					if (CheckIfObjectExists("repository",rep.getId(),name)) {
						throw new RuntimeException("A Repository with that name already exists in the same domain"); 
					}
				} else
				if (obj instanceof Datasource) {
					Datasource ds = (Datasource)obj;
					if (CheckIfObjectExists("datasource",ds.getId(),name)) {
						throw new RuntimeException("A Data Source with that name already exists in the same domain"); 
					}
				} else
				if (obj instanceof Credential) {
					Credential cred = (Credential)obj;
					if (CheckIfObjectExists("credentials",cred.getId(),name)) {
						throw new RuntimeException("A Credential with that name already exists in the same domain"); 
					}
				} else
				if (obj instanceof Action) {
					Action act = (Action)obj;
					if (CheckIfObjectExists("action",act.getId(),name)) {
						//
						// Special processing - we need to find what "kind" the other object is
						// and report that. Otherwise, it could be very confusing...
						//
						try {
							PreparedStatement st = getDBConnection().prepareStatement("SELECT id FROM dm.dm_action WHERE domainid=? AND name=?");
							st.setInt(1, act.getDomainId());
							st.setString(2, name);
							ResultSet rs = st.executeQuery();
							if (rs.next()) {
								int otherid = rs.getInt(1);
								rs.close();
								Action otheract = getAction(otherid,true);
								if (otheract.isFunction()) throw new RuntimeException("A function with that name already exists in the same domain"); 
								if (otheract.getKind() == ActionKind.GRAPHICAL) throw new RuntimeException("An action with that name already exists in the same domain");
								throw new RuntimeException("A procedure with that name already exists in the same domain");
							}
							rs.close();
						} catch (SQLException ex) {
							throw new RuntimeException(ex.getMessage());
						}
						throw new RuntimeException("An object with that name already exists in the same domain");	// should never hit here, but just in case...
						
					}
				} else
				if (obj instanceof Notify) {
					Notify notify = (Notify)obj;
					if (CheckIfObjectExists("notify",notify.getId(),name)) {
						throw new RuntimeException("A Notifier with that name already exists in the same domain"); 
					}
				}
				if (obj instanceof NotifyTemplate) {
					NotifyTemplate template = (NotifyTemplate)obj;
					Notify notify = getNotify(template.getNotifierId(),true);
					int domainid = notify.getDomainId();
					String sql = "SELECT t.notifierid FROM dm.dm_template t WHERE t.name=? AND t.id<>? AND t.notifierid IN (SELECT x.id FROM dm.dm_notify x WHERE x.domainid=?)";
					try {
						PreparedStatement st = getDBConnection().prepareStatement(sql);
						st.setString(1, name);
						st.setInt(2,template.getId());
						st.setInt(3, domainid);		
						ResultSet rs = st.executeQuery();
						if (rs.next()) {
							int notifierid=rs.getInt(1);
							Notify other = getNotify(notifierid,false);
							rs.close();
							st.close();
							if (other != null) {
								throw new RuntimeException("A template with that name already exists on notifier '"+other.getName()+"' in the same domain");
							} else {
								throw new RuntimeException("A template with that name already exists in the same domain");
							}
						}
						rs.close();
						st.close();
					} catch (SQLException ex) {
						throw new RuntimeException(ex.getMessage());
					}
				} else
				if (obj instanceof Application) {
					Application app = (Application)obj;
					if (CheckIfObjectExists("application",app.getId(),name)) {
						//
						// Special processing - an application can also be a release
						//
						try {
							PreparedStatement st = getDBConnection().prepareStatement("SELECT id FROM dm.dm_application WHERE domainid=? AND name=?");
							st.setInt(1, app.getDomainId());
							st.setString(2, name);
							ResultSet rs = st.executeQuery();
							if (rs.next()) {
								int otherid = rs.getInt(1);
								rs.close();
								Application otherapp = getApplication(otherid,true);
								if (otherapp.getIsRelease().equalsIgnoreCase("y")) throw new RuntimeException("A Release with that name already exists in the same domain");
								throw new RuntimeException("An Application with that name already exists in the same domain");
							}
							rs.close();
						} catch (SQLException ex) {
							throw new RuntimeException(ex.getMessage());
						}
						throw new RuntimeException("An Application with that name already exists in the same domain"); // should never hit this but be safe...
					}
				} else
				if (obj instanceof Component) {
					Component comp = (Component)obj;
					if (CheckIfObjectExists("component",comp.getId(),name)) {
						throw new RuntimeException("A Component with that name already exists in the same domain"); 
					}
				}
		    
				update.add(", name = ?", name);
				return true;
			}}
			throw new RuntimeException("Name field is mandatory - please enter a value");
		case SUMMARY: update.add(", summary = ?", changes.get(field)); return true;
		case OWNER: {
			DMObject owner = (DMObject) changes.get(field);
			if(owner != null) {
				switch(owner.getObjectType()) {
				case USER:		update.add(", ownerid = ?, ogrpid = ?", owner.getId(), Null.INT); return true;
				case USERGROUP:	update.add(", ownerid = ?, ogrpid = ?", Null.INT, owner.getId()); return true;
				default: break; // This will have been checked earlier
				}
			} else {
				update.add(", ownerid = ?, ogrpid = ?", Null.INT, Null.INT);
				return true;
			}}
			break;
		case ENGINE_HOSTNAME:
		{
   if (obj instanceof Domain)
   {
    Domain dom = (Domain)obj;
  
		  Engine engine = getEngine4Domain(dom.getId());
		  if (engine == null)
		  {
		   String name = (String) changes.get(field);
		   engine = createEngine(dom.getId(),name);
		  }
		  engine.updateSummary(changes);
   }  
		}
		break;
		
		default: System.err.println("ERROR: Unhandled object summary field " + field); break;
		}
		return false;
	}
	
	
	public boolean updateApplication(Application app, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_application ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case PRE_ACTION: {
				DMObject action = (DMObject) changes.get(field);
				update.add(", preactionid = ?", (action != null) ? action.getId() : Null.INT);
				}
				break;
			case POST_ACTION: {
				DMObject action = (DMObject) changes.get(field);
				update.add(", postactionid = ?", (action != null) ? action.getId() : Null.INT);
				}
				break;
			case CUSTOM_ACTION: {
				DMObject action = (DMObject) changes.get(field);
				update.add(", actionid = ?", (action != null) ? action.getId() : Null.INT);
				}
				break;
			case SUCCESS_TEMPLATE: {
				DMObject template = (DMObject) changes.get(field);
				update.add(", successtemplateid = ?", (template != null) ? template.getId() : Null.INT);
				}
				break;
			case FAILURE_TEMPLATE: {
				DMObject template = (DMObject) changes.get(field);
				update.add(", failuretemplateid = ?", (template != null) ? template.getId() : Null.INT);
				}
				break;

			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(app, update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", app.getId());
		
		try {
			update.execute();
			RecordObjectUpdate(app,changes);
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateBuilder(Builder builder, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_buildengine ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(builder, update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", builder.getId());
		
		try {
			update.execute();
			// RecordObjectUpdate(builder,changes);
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	public Component getComponent(int compid, boolean detailed)
	{
		if (compid < 0) {
			Component ret = new Component(this, compid, "");
			ret.setName("");
			ret.setSummary("");
			ret.setRollup(ComponentFilter.OFF);
			ret.setRollback(ComponentFilter.OFF);
			ret.setAlwaysDeploy(false);
			ret.setDeploySequentially(false);
			ret.setBaseDirectory("");
			ret.setFilterItems(true);
			ret.setPreAction(new Action(this, 0, ""));
			ret.setPostAction(new Action(this, 0, ""));
			ret.setCustomAction(new Action(this, 0, ""));
			ret.setLastBuildNumber(0);
			return ret; 
		}
	 
		String sql = null;
		if(detailed) {
			sql = "SELECT a.name, a.summary, a.predecessorid, a.parentid, a.domainid, a.branch, a.status, "
				+ "  a.rollup, a.rollback, a.filteritems, a.deployalways,a.deploysequentially, a.basedir, a.datasourceid, a.buildjobid, a.lastbuildnumber, "
				+ "  uc.id, uc.name, uc.realname, a.created, "					// 17 - 20
				+ "  um.id, um.name, um.realname, a.modified, "					// 21 - 24
				+ "  uo.id, uo.name, uo.realname, g.id, g.name, "				// 25 - 29
				+ "  a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid, " // 30 - 35
				+ "  a3.id, a3.name, a3.domainid, "							 	// 36 - 38
				+ "  a.xpos, a.ypos, t.id, t.name, "								 // 39 - 42
				+ "  c.id, c.name "
				+ "FROM dm.dm_component a "
				+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
				+ "LEFT OUTER JOIN dm.dm_user uo ON a.ownerid = uo.id "			// owner user
				+ "LEFT OUTER JOIN dm.dm_usergroup g ON a.ogrpid = g.id "		// owner group
				+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
				+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
				+ "LEFT OUTER JOIN dm.dm_action a3 ON a.actionid = a3.id "		// custom action
				+ "LEFT OUTER JOIN dm.dm_type t ON t.id = a.comptypeid "  		// Component Types
				+ "LEFT OUTER JOIN dm.dm_component_categories fc on a.id = fc.id "
				+ "LEFT OUTER JOIN dm.dm_category c ON c.id = fc.categoryid "  // category
				+ "WHERE  a.status = 'N' and a.id = ?";	
		} else {
			sql = "SELECT a.name, a.summary, a.predecessorid, a.parentid, a.domainid, a.branch, a.status, "
				+ "  a.rollup, a.rollback, a.filteritems, a.deployalways, a.deploysequentially, a.basedir, a.datasourceid, a.buildjobid, a.lastbuildnumber "
				+ "FROM dm.dm_component a WHERE  a.status = 'N' and a.id = ?";	
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, compid);
			ResultSet rs = stmt.executeQuery();
			Component ret = null;
			if(rs.next()) {
				ret = new Component(this, compid, rs.getString(1));
				ret.setSummary(getString(rs, 2, ""));
				ret.setPredecessorId(getInteger(rs, 3, 0));
				ret.setParentId(getInteger(rs, 4, 0));
				ret.setDomainId(getInteger(rs,5,0));
				String thislabel = getString(rs,6,"");
				ret.setLabel(thislabel);
				if (thislabel.length() == 0) {
					// No label for this version
					String ParentLabel = getParentLabel(1,getInteger(rs,3,0));
					ret.setParentLabel(ParentLabel);
				}
				getStatus(rs, 7, ret);
				ret.setRollup(ComponentFilter.fromInt(getInteger(rs, 8, 0)));
				ret.setRollback(ComponentFilter.fromInt(getInteger(rs, 9, 0)));
				ret.setFilterItems(getBoolean(rs, 10, false));
				ret.setAlwaysDeploy(getBoolean(rs, 11, false));
				ret.setDeploySequentially(getBoolean(rs, 12, false));
				ret.setBaseDirectory(getString(rs, 13, ""));
				int dsid = getInteger(rs,14,0);
				if (dsid>0) {
					Datasource ds = getDatasource(dsid,true);
					ret.setDatasource(ds);
				}
				int bjid = getInteger(rs,15,0);
				if (bjid>0) {
					BuildJob bj = getBuildJob(bjid);
					ret.setBuildJob(bj);
				}
				int lastbuildid = getInteger(rs,16,0);
				ret.setLastBuildNumber(lastbuildid);
				
				if(detailed) {
					getCreatorModifierOwner(rs, 17, ret);
					getPreAndPostActions(rs, 30, ret);
					int custactionid = getInteger(rs, 36, 0);
					if(custactionid != 0) {
						Action ca = getAction(custactionid,true);
						ret.setCustomAction(ca);
					}
					ret.setXpos(getInteger(rs,39,0));
					ret.setYpos(getInteger(rs,40,0));
					ret.setComptypeId(getInteger(rs,41,0));
					ret.setComptype(getString(rs,42,""));
					Category cat = new Category(getInteger(rs,43,0),getString(rs,44,""));
					ret.setCategory(cat);
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		// throw new RuntimeException("Unable to retrieve component " + compid + " from database");	
		return null;
	}
	
	
	public boolean updateComponent(Component comp, SummaryChangeSet changes)
	{
		Category cat = null;
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_component ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case PRE_ACTION: {
				DMObject action = (DMObject) changes.get(field);
				update.add(", preactionid = ?", (action != null) ? action.getId() : Null.INT);
				}
				break;
			case POST_ACTION: {
				DMObject action = (DMObject) changes.get(field);
				update.add(", postactionid = ?", (action != null) ? action.getId() : Null.INT);
				}
				break;
			case CUSTOM_ACTION: {
				DMObject action = (DMObject) changes.get(field);
				update.add(", actionid = ?", (action != null) ? action.getId() : Null.INT);
				}
				break;
			case ACTION_CATEGORY:
				cat = (Category) changes.get(field);
				break;
			case COMPTYPE: {
				String tmp = (String) changes.get(field);
				Integer comptype = null;
				if (tmp != null)
					comptype = new Integer(tmp);
    
				update.add(", comptypeid = ?", (comptype != null) ? comptype.intValue() : Null.INT);
				}
				break;
			case FILTER_ITEMS: {
				boolean fi = false;
				String sfi = (String) changes.get(field);
				if(sfi != null) {
					fi = sfi.equalsIgnoreCase("Items");
				}
				update.add(", filteritems = ?", (fi ? "Y" : "N"));
				}
				break;
			case ROLLUP:
			case ROLLBACK: {
				ComponentFilter cf = ComponentFilter.OFF;
				String scf = (String) changes.get(field);
				if(scf != null) {
					if(scf.equalsIgnoreCase("ON")) {
						cf = ComponentFilter.ON;
					} else if(scf.equalsIgnoreCase("ALL")) {
						cf = ComponentFilter.ALL;
					}
				}
				if(field == SummaryField.ROLLUP) {
					update.add(", rollup = ?", cf.value());
				} else {
					update.add(", rollback = ?", cf.value());					
				}}
				break;
			case DEPLOY_ALWAYS:
				update.add(", deployalways = ?", changes.getBoolean(field));
				break;
			case DEPLOY_SEQUENTIALLY:
				update.add(", deploysequentially = ?", changes.getBoolean(field));
				break;
			case BASE_DIRECTORY:
				update.add(", basedir = ?",(String)changes.get(field));
				break;
			case COMP_BUILDJOB:
				BuildJob bj = (BuildJob) changes.get(field);
				update.add(", buildjobid = ?", (bj != null) ? bj.getId() : Null.INT);
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(comp,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", comp.getId());
		
		try {
			update.execute();
			
   if(cat != null) 
   {  
    String sql1="DELETE from dm.dm_component_categories where id = ?";  // only allow 1 category at this time
    PreparedStatement stmt = getDBConnection().prepareStatement(sql1);
    stmt.setInt(1, comp.getId());
    stmt.execute();
    stmt.close();
    addToCategory(cat.getId(), comp.getOtid());
   }
   			RecordObjectUpdate(comp,changes);
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateTask(Task task, SummaryChangeSet changes)
	{
		System.out.println("updateTask start");
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_task ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			System.out.println("field="+field);
			switch(field) {
			case PRE_ACTION: {
				DMObject action = (DMObject) changes.get(field);
				update.add(", preactionid = ?", (action != null) ? action.getId() : Null.INT);
				}
				break;
			case POST_ACTION: {
				DMObject action = (DMObject) changes.get(field);
				update.add(", postactionid = ?", (action != null) ? action.getId() : Null.INT);
				}
				break;
			case TASK_SHOWOUTPUT: {
				Boolean logOutput = (Boolean) changes.get(field);
				update.add(", logoutput = ?", ((logOutput != null) && logOutput.booleanValue()) ? "Y" : "N");
				}
				break;
			case TASK_AVAILABLE_TO_SUBDOMAINS: {
				Boolean subDomains = (Boolean) changes.get(field);
				update.add(",  subdomains = ?", ((subDomains != null) && subDomains.booleanValue()) ? "Y" : "N");
				}
				break;
			case SUCCESS_TEMPLATE: {
				DMObject template = (DMObject) changes.get(field);
				update.add(", successtemplateid = ?", (template != null) ? template.getId() : Null.INT);
				}
				break;
			case FAILURE_TEMPLATE: {
				DMObject template = (DMObject) changes.get(field);
				update.add(", failuretemplateid = ?", (template != null) ? template.getId() : Null.INT);
				}
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(task,update, field, changes);
				} else {
					System.err.println("ERROR: ** Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", task.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		System.out.println("updateTask end");
		return false;
	}
	
	
	public ComponentItem getComponentItem(int ciid, boolean detailed)
	{
		String sql = null;
		
		System.out.println("getComponentItem, ciid="+ciid);		// +" compid="+compid
		
		if(detailed) {
			sql = "SELECT a.name, a.summary, b.domainid, a.rollup, a.rollback, a.predecessorid, a.status, "
				+ "  a.compid, a.repositoryid, a.target, "
				+ "  uc.id, uc.name, uc.realname, a.created, "
				+ "  um.id, um.name, um.realname, a.modified, "
				+ "  a.xpos, a.ypos "
				+ "FROM dm.dm_componentitem a "
				+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
				+ "LEFT OUTER JOIN dm.dm_component b ON a.compid=b.id "			// domain
				+ "WHERE a.id = ?";		//  AND a.compid=?	
		} else {
			sql = "SELECT a.name, a.summary, b.domainid, a.rollup, a.rollback, a.predecessorid, a.status "
				+ "FROM dm.dm_componentitem a "
				+ "LEFT OUTER JOIN dm.dm_component b ON a.compid=b.id "			// domain
				+ "WHERE a.id = ?";		//  AND a.compid=?	
		}
		
		System.out.println("sql="+sql);
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, ciid);
			//stmt.setInt(2, compid);
			ResultSet rs = stmt.executeQuery();
			ComponentItem ret = null;
			if(rs.next()) {
				ret = new ComponentItem(this, ciid, rs.getString(1));
				ret.setSummary(rs.getString(2));
				ret.setDomainId(getInteger(rs, 3, 0));
				ret.setRollup(ComponentFilter.fromInt(getInteger(rs, 4, 0)));
				ret.setRollback(ComponentFilter.fromInt(getInteger(rs, 5, 0)));
				ret.setPredecessorId(getInteger(rs, 6, 0));
				getStatus(rs, 7, ret);
				if(detailed) {
					int compid = getInteger(rs, 8, 0);
					if(compid != 0) {
						ret.setParent(getComponent(compid, false));
					}
					int repoid = getInteger(rs, 9, 0);
					if(repoid != 0) {
						ret.setRepository(getRepository(repoid, false));
					}
					ret.setTargetDir(rs.getString(10));
					getCreatorModifier(rs, 11, ret);
					ret.setXpos(getInteger(rs, 19, 0));
					ret.setYpos(getInteger(rs, 20, 0));
				}
			} else {
				System.out.println("No rows retrieved");
			}
			
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve component item " + ciid + " from database");	
	}

	
	public boolean updateComponentItemSummary(ComponentItem ci, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_componentitem ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case ROLLUP:
			case ROLLBACK: {
				ComponentFilter cf = ComponentFilter.OFF;
				String scf = (String) changes.get(field);
				if(scf != null) {
					if(scf.equalsIgnoreCase("ON")) {
						cf = ComponentFilter.ON;
					} else if(scf.equalsIgnoreCase("ALL")) {
						cf = ComponentFilter.ALL;
					}
				}
				if(field == SummaryField.ROLLUP) {
					update.add(", rollup = ?", cf.value());
				} else {
					update.add(", rollback = ?", cf.value());					
				}}
				break;
			case ITEM_REPOSITORY: {
				DMObject repo = (DMObject) changes.get(field);
				update.add(", repositoryid = ?", (repo != null) ? repo.getId() : Null.INT);
				}
				break;
			case ITEM_TARGETDIR: {
				String targetDir = (String) changes.get(field);
				update.add(", target = ?", (targetDir != null) ? targetDir : Null.STRING);
				}
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(ci,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", ci.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}

	
	public List<DMProperty> getPropertiesForComponentItem(ComponentItem ci)
	{
		String sql = "SELECT p.name, p.value, p.encrypted, p.overridable, p.appendable "
				+ "FROM dm.dm_compitemprops p WHERE p.compitemid = ? ORDER BY 1";	
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, ci.getId());
			ResultSet rs = stmt.executeQuery();
			List<DMProperty> ret = new ArrayList<DMProperty>();
			while(rs.next()) {
				ret.add(new DMProperty(rs.getString(1), rs.getString(2), getBoolean(rs, 3), getBoolean(rs, 4), getBoolean(rs, 5)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve properties for ComponentItem " + ci.getId() + " from database");				
	}

	
	public boolean updateComponentItemProperties(ComponentItem ci, ACDChangeSet<DMProperty> changes)
	{
		return internalUpdateProperties("dm_compitem", "compitemid", ci.getId(), ci.getDomain(),changes);
	}

	
	private void addApplicationVersions(Application app,List<Application> applist)
	{
		String sql = "SELECT id, isRelease FROM dm.dm_application WHERE predecessorid=? and status='N'";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, app.getId());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Application a = getApplication(rs.getInt(1),true);
				a.setIsRelease(getString(rs,2,"N"));
				applist.add(a);
				// Recurse
				addApplicationVersions(a,applist);
			}
			rs.close();
			stmt.close();
			return;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve application versions for app " + app.getId() + " from database");	
	}
	
	
	public List<Application> getApplicationVersions(Application app)
	{
		List<Application> ret = new ArrayList<Application>();
		addApplicationVersions(app,ret);
		return ret;
		
	}
	
	
	private void addComponentVersions(Component comp,List<Component> complist)
	{
		String sql = "SELECT id FROM dm.dm_component WHERE predecessorid=? and status='N' ORDER BY id";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, comp.getId());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Component c = getComponent(rs.getInt(1),true);
				complist.add(c);
				// Recurse
				addComponentVersions(c,complist);
			}
			rs.close();
			stmt.close();
			return;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve component versions for comp " + comp.getId() + " from database");	
	}
	
	private void addActionVersions(Action action,List<Action> actionlist)
	{
		String sql = "SELECT id FROM dm.dm_action WHERE parentid=? and status='A' ORDER BY id";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, action.getId());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Action a = getAction(rs.getInt(1),true);
				actionlist.add(a);
			}
			rs.close();
			stmt.close();
			return;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action versions for action " + action.getId() + " from database");	
	}
	
	
	public List<Component> getComponentVersions(Component comp)
	{
		List<Component> ret = new ArrayList<Component>();
		addComponentVersions(comp,ret);
		return ret;
	}
	
	public List<Action> getActionVersions(Action action)
	{
		List<Action> ret = new ArrayList<Action>();
		addActionVersions(action,ret);
		return ret;
	}
	
	public List<Environment> getEnvironmentsForServer(Server server)
	{
		List<Environment> res = new ArrayList<Environment>();
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement("SELECT envid FROM dm.dm_serversinenv WHERE serverid=?");
			stmt.setInt(1,server.getId());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				Environment env = getEnvironment(rs.getInt(1),true);
				res.add(env);
			}
			rs.close();
			stmt.close();
			return res;
		} catch(SQLException ex) {
			System.out.println("Failed selecting environments for server "+server.getId());
			return null;
		}
	}
	
	// Used by both the deploy dialog and the API for deployments
	public List<Environment> getEnvironmentsForApplication(Application app,Domain dom)
	{
		// The distinct is necessary as it is possible to put both the
		// application and appver into appsallowedinenv, even though the UI
		// only lets you enter applications
		String DomainList=((Integer)dom.getId()).toString();
		
		boolean isRelease = (app.getObjectType()==ObjectType.RELVERSION || app.getObjectType()==ObjectType.RELEASE);
			
		try
		{
			//
			// We only want to list environments which are either in the same domain as the
			// task being executed or in one of its sub-domains OR (new requirement) one of
			// the environments in domains PRIOR to this one (if we're in a lifecycle)
			//
			boolean inLifecycle=false;
			int pdid=0;
			Domain parentDomain = dom.getDomain();
			System.out.println("ourDomain=["+dom.getName()+"]");
			if (parentDomain != null) System.out.println("parentDomain=["+parentDomain.getName()+"]");
			if (parentDomain != null && parentDomain.getLifecycle()) {
				// Yes we're in a lifecycle
				System.out.println("In lifecycle");
				inLifecycle=true;
				pdid=parentDomain.getId();
			}
			//
			// Find any sub-domains for the task domainand add those to the domain list
			//
			Map<Integer, String> mapsub = new HashMap<Integer, String>();
			GetSubDomains(mapsub,dom.getId());
			Iterator<Map.Entry<Integer,String>> it = mapsub.entrySet().iterator();
			while (it.hasNext())
			{
			    Map.Entry<Integer,String> pairs = it.next();
			    DomainList=DomainList+","+pairs.getKey();
			}
			if (inLifecycle) {
				// Now add any sibling domains that are prior to this one
				String sqld = "SELECT id FROM dm_domain WHERE domainid=? AND position<"
						+ "(select position FROM dm.dm_domain WHERE id=?)";
				PreparedStatement psd = getDBConnection().prepareStatement(sqld);
				System.out.println(sqld);
				System.out.println("pdid="+pdid);
				System.out.println("taskdomain="+dom.getId());
				psd.setInt(1,pdid);
				psd.setInt(2,dom.getId());
				ResultSet rsd = psd.executeQuery();
				System.out.println("About to loop");
				while (rsd.next()) {
					System.out.println("got "+rsd.getInt(1));
					DomainList=DomainList+","+rsd.getInt(1);
				}
				System.out.println("done looping");
				rsd.close();
			}
			
			int numApplications=0;	// for releases
			String sqlc = "SELECT count(*) FROM dm.dm_applicationcomponentflows WHERE appid=?";
			String sql2;
			if (!isRelease) {
				sql2 = "SELECT DISTINCT e.id, e.name, e.summary "
						+ "FROM dm.dm_environment e, dm.dm_appsallowedinenv aie, dm.dm_application a "
						+ "WHERE a.id = ? AND (aie.appid = a.id OR aie.appid = a.parentid) "
						+ "AND e.id = aie.envid AND e.status = 'N' AND e.domainid IN ("+DomainList+")";
			} else {
				System.out.println("isRelease");
				sql2 = "SELECT e.id,e.name,e.summary "
						+ "FROM dm.dm_environment e, dm.dm_appsallowedinenv aie, dm.dm_application a "
						+ "WHERE e.id = aie.envid AND e.status='N' "
						+ "AND a.id in "
						+ "( "
						+ "SELECT objto FROM dm.dm_applicationcomponentflows WHERE appid=? "
						+ "UNION "
						+ "SELECT objfrom FROM dm.dm_applicationcomponentflows WHERE appid=? "
						+ ") "
						+ "AND (aie.appid = a.id OR aie.appid= a.parentid) "
						+ "GROUP BY e.id,e.name,e.summary HAVING count(*)=?";
			}
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			stmt2.setInt(1, app.getId());
			if (isRelease) {
				stmt2.setInt(2, app.getId());
				PreparedStatement stmtc = getDBConnection().prepareStatement(sqlc);
				stmtc.setInt(1, app.getId());
				ResultSet rsc = stmtc.executeQuery();
				if (rsc.next()) {
					numApplications = rsc.getInt(1);
				}
				rsc.close();
				stmtc.close();
				System.out.println("numApplications="+numApplications);
				stmt2.setInt(3,numApplications);
			}
			ResultSet rs2 = stmt2.executeQuery();
			List<Environment> ret = new ArrayList<Environment>();
			while(rs2.next()) {
				Environment env = getEnvironment(rs2.getInt(1), true);
				
				System.out.println("*************************");
				System.out.println("env="+env.getName());
				System.out.println("isWriteable="+env.isWriteable());
				System.out.println("*************************");
				if (env.isWriteable()) {
					env.setSummary(rs2.getString(3));
					ret.add(env);
				}
			}
			rs2.close();
			stmt2.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve environment " + app.getId() + " from database");				
	}
	
	public List<Environment> getEnvironmentsForApplication(Application app,int taskid)
	{
		
		String sql1 = "SELECT domainid FROM dm.dm_task WHERE id=?";
		List<Environment> ret = null;
		try {
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1, taskid);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				int taskdomain=rs1.getInt(1);
				
				//
				// Check if this is part of a lifecycle by checking the parent domain
				//
				Domain ourDomain = getDomain(taskdomain);
				ret = getEnvironmentsForApplication(app,ourDomain);
			}
			rs1.close();
			stmt1.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve environment " + app.getId() + " from database");	
	}
	
	public List<Environment> getDeployedEnvironmentsForApplication(Application app,int taskid)
	{
		// The distinct is necessary as it is possible to put both the
		// application and appver into appsallowedinenv, even though the UI
		// only lets you enter applications
		
		String DomainList="";
		
		Integer taskdomain=0;
		
		
		try
		{
			//
			// We only want to list environments which are either in the same domain as the
			// task being executed or in one of its sub-domains.
			//
			String sql1 = "SELECT domainid FROM dm.dm_task WHERE id=?";
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1, taskid);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				taskdomain=rs1.getInt(1);
				DomainList=taskdomain.toString();
			}
			rs1.close();
			stmt1.close();
			//
			// Find any sub-domains for the task domainand add those to the domain list
			//
			Map<Integer, String> mapsub = new HashMap<Integer, String>();
			GetSubDomains(mapsub,taskdomain);
			Iterator<Map.Entry<Integer,String>> it = mapsub.entrySet().iterator();
			while (it.hasNext())
			{
			    Map.Entry<Integer,String> pairs = it.next();
			    DomainList=DomainList+","+pairs.getKey();
			}
			
			String sql2 = "SELECT DISTINCT e.id, e.name, e.summary "
					+ "FROM 		dm.dm_environment e,dm.dm_appsinenv aie "
					+ "WHERE 		aie.appid in (SELECT x.id FROM dm.dm_application x where ? in (x.id,x.parentid)) "
					+ "AND		e.id = aie.envid "
					+ "AND		e.status = 'N' "
					+ "AND		e.domainid IN ("+DomainList+")";
			
			System.out.println("sql2="+sql2);
			
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			stmt2.setInt(1, app.getId());
			ResultSet rs2 = stmt2.executeQuery();
			List<Environment> ret = new ArrayList<Environment>();
			while(rs2.next()) {
				Environment env = getEnvironment(rs2.getInt(1), true);
				
				System.out.println("*************************");
				System.out.println("env="+env.getName());
				System.out.println("isWriteable="+env.isWriteable());
				System.out.println("*************************");
				if (env.isWriteable()) {
					env.setSummary(rs2.getString(3));
					ret.add(env);
				}
			}
			rs2.close();
			stmt2.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve environment " + app.getId() + " from database");	
	}
	
	public boolean updateEnvironment(Environment env, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_environment ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case AVAILABILITY:
				update.add(", calusage = ?", (String)changes.get(field));
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(env,update, field, changes);
				} else {
					System.err.println("ERROR: Unhanddled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", env.getId());
		
		try {
			RecordObjectUpdate(env,changes);
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public ReportDataSet getTimeToDeployForApplication(int appid)
	{
		String sql = "SELECT d.deploymentid, d.finished - d.started "
			+ "FROM dm.dm_deployment d WHERE d.appid = ? order by d.deploymentid desc";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, appid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs,10);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve deployment time report from database");			
	}
	
	public ReportDataSet getTimeToDeployForServer(int servid)
	{	
		String sql = 	"SELECT		st.deploymentid,sum(st.finished-st.started)	"
				+		"FROM		dm.dm_deploymentstep st,	"
				+		"			dm.dm_deploymentxfer xf		"
				+		"WHERE		st.deploymentid = xf.deploymentid	"
				+		"AND		st.stepid = xf.stepid	"
				+		"AND		xf.serverid = ?	"
				+		"GROUP BY 	st.deploymentid	"
				+		"ORDER BY 	st.deploymentid DESC";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, servid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs,10);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve deployment time report from database");			
	}
	
	public ReportDataSet getTimeToDeployForEnvironment(int envid)
	{
		String sql = "SELECT d.deploymentid, d.finished - d.started "
				+ "FROM dm.dm_deployment d WHERE d.envid = ? order by d.deploymentid desc";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, envid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs,10);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve deployment time report from database");			
	}

	
	public JSONObject getTimelineForEnvironment(int envid)
	{
		long now = timeNow();
		JSONObject ret = new JSONObject();
		Environment env = getEnvironment(envid,true);
		String sql =
		"select	a.appid,b.name,coalesce(b.parentid,b.id),a.started,c.name	" +
		"from	dm.dm_deployment	a,	" +
		"		dm.dm_application	b,	" +
		"		dm.dm_application	c	" +
		"		where	a.envid=?	" +
		"		and	b.id=a.appid	" +
		"		and	c.id=coalesce(b.parentid,b.id)	" +
		"		order by 3,4";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,envid);
			ResultSet rs = stmt.executeQuery();
			int lastappid = -1;
			String lastappname="";
			int lastparentid = -1;
			JSONArray groups = new JSONArray();
			JSONArray items = new JSONArray();
			long st=0;
			while (rs.next()) {
				long et=0;
				int appid = rs.getInt(1);
				String appname = rs.getString(2);
				int parentid = rs.getInt(3);
				long t = rs.getLong(4);
				String groupname = rs.getString(5);	// Parent app (for grouping)
				et = t;
				if (appid != lastappid) {
					if (parentid != lastparentid) {
						// New Base Application - create a group for this base application
						JSONObject group = new JSONObject();
						group.add("id", parentid);
						group.add("name",groupname);	
						groups.add(group);
						et = now;
					}
					if (lastappid != -1) {
						JSONObject appobj = new JSONObject();
						appobj.add("id",lastappid);
						appobj.add("name",lastappname);
						appobj.add("from",st);
						appobj.add("to",et);
						appobj.add("group",lastparentid);
						items.add(appobj);
						// st = t;
					}
					st = t;
					lastparentid = parentid;
					lastappid = appid;
					lastappname = appname;
				}
			}
			// last item needs a final entry to take it to current date/time
			JSONObject appobj = new JSONObject();
			appobj.add("id",lastappid);
			appobj.add("name",lastappname);
			appobj.add("from",st);
			appobj.add("to",now);
			appobj.add("group",lastparentid);
			items.add(appobj);
			ret.add("created", env.getCreated());
			ret.add("groups", groups);
			ret.add("items", items);
			return ret;
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve environment timeline from database");	
	}
	
	public JSONObject getTimelineForServer(int servid)
	{
		long now = timeNow();
		JSONObject ret = new JSONObject();
		Server serv = getServer(servid,true);
		String sql =
		"select	distinct d.componentid,b.name,coalesce(b.parentid,b.id),a.started,c.name	" +
		"from	dm.dm_deployment		a,	" +
		"		dm.dm_component			b,	" +
		"		dm.dm_component			c,	" +
		"		dm.dm_deploymentxfer	d	" +
		"where	d.serverid=?	" +
		"and	d.deploymentid=a.deploymentid	" +
		"and	b.id=d.componentid	" +
		"and	c.id=coalesce(b.parentid,b.id)	" +
		"order by 3,4";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,servid);
			ResultSet rs = stmt.executeQuery();
			int lastcompid = -1;
			String lastcompname="";
			int lastparentid = -1;
			JSONArray groups = new JSONArray();
			JSONArray items = new JSONArray();
			long st=0;
			while (rs.next()) {
				long et=0;
				int compid = rs.getInt(1);
				String compname = rs.getString(2);
				int parentid = rs.getInt(3);
				long t = rs.getLong(4);
				String groupname = rs.getString(5);	// Parent app (for grouping)
				et = t;
				if (compid != lastcompid) {
					if (parentid != lastparentid) {
						// New Base Application - create a group for this base application
						JSONObject group = new JSONObject();
						group.add("id", parentid);
						group.add("name",groupname);	
						groups.add(group);
						et = now;
					}
					if (lastcompid != -1) {
						JSONObject appobj = new JSONObject();
						appobj.add("id",lastcompid);
						appobj.add("name",lastcompname);
						appobj.add("from",st);
						appobj.add("to",et);
						appobj.add("group",lastparentid);
						items.add(appobj);
						// st = t;
					}
					st = t;
					lastparentid = parentid;
					lastcompid = compid;
					lastcompname = compname;
				}
			}
			// last item needs a final entry to take it to current date/time
			JSONObject appobj = new JSONObject();
			appobj.add("id",lastcompid);
			appobj.add("name",lastcompname);
			appobj.add("from",st);
			appobj.add("to",now);
			appobj.add("group",lastparentid);
			items.add(appobj);
			ret.add("created", serv.getCreated());
			ret.add("groups", groups);
			ret.add("items", items);
			return ret;
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve server timeline from database");	
	}
	
	public ReportDataSet getSuccessFailureForApplication(int appid)
	{
		String sql = "SELECT 'success', count(*) FROM dm.dm_deployment d WHERE d.appid = ? AND d.exitcode = 0 "
			+ "UNION SELECT 'failure', count(*) FROM dm.dm_deployment d WHERE d.appid = ? AND d.exitcode <> 0 "
			+ "ORDER BY 1 DESC";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, appid);
			stmt.setInt(2, appid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve success failure report from database");			
	}
	
	public ReportDataSet getSuccessFailureForEnvironment(int envid)
	{
		String sql = "SELECT 'success', count(*) FROM dm.dm_deployment d WHERE d.envid = ? AND d.exitcode = 0 "
			+ "UNION SELECT 'failure', count(*) FROM dm.dm_deployment d WHERE d.envid = ? AND d.exitcode <> 0 "
			+ "ORDER BY 1 DESC";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, envid);
			stmt.setInt(2, envid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve success failure report from database");			
	}
	
	
	// Server
	
	public ReportDataSet getSuccessFailureForServer(int servid)
	{
		String sql = 
				"SELECT 'success', count(*) FROM dm.dm_deployment d WHERE d.exitcode = 0 AND d.deploymentid IN (SELECT x.deploymentid FROM dm.dm_deploymentxfer x WHERE x.serverid=?) "
			+ 	"UNION "
			+	"SELECT 'failure', count(*) FROM dm.dm_deployment d WHERE d.exitcode <> 0 AND d.deploymentid IN (SELECT x.deploymentid FROM dm.dm_deploymentxfer x WHERE x.serverid=?) "
			+ 	"ORDER BY 1 DESC";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, servid);
			stmt.setInt(2, servid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve success failure report from database");			
	}
	
	public CompType getServerCompTypeDetail(int comptypeid)
 {
  if (comptypeid < 0)
  {
   CompType ret = new CompType(this, comptypeid, "", "N", "N",1);
 
   return ret;  
  }
  
  String sql = null;
  sql = "SELECT s.name, s.database, s.deletedir, s.domainid, s.status "
    + "FROM dm.dm_type s WHERE s.id = ?"; 
  
  try
  {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, comptypeid);
   ResultSet rs = stmt.executeQuery();
   CompType ret = null;
   if(rs.next()) {
    ret = new CompType(this, comptypeid, rs.getString(1),rs.getString(2),rs.getString(3),rs.getInt(4));
   }
   rs.close();
   stmt.close();
   if(ret != null) {
    return ret;
   }
  }
  catch(SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  throw new RuntimeException("Unable to retrieve comptypeid " + comptypeid + " from database");    
 }
 
	
	public Server getServer(int serverid, boolean detailed)
	{
	 if (serverid < 0)
	 {
		 Server ret = new Server(this, serverid, "");
		 ret.setSummary("");
		 ret.setHostName("");
		 ret.setProtocol("");
		 ret.setBaseDir("");
		 ret.setSSHPort(22);	// default
		 ret.setServerType(new ServerType(
				 0, "",
				 LineEndFormat.fromInt(0),""));
		 ret.setCredential(new Credential(this, 0, ""));  
		 return ret;  
	 }
	 
		String sql = null;
		if(detailed) {
			sql = "SELECT s.name, s.summary, s.domainid, s.status, "
				+ "  s.hostname, s.protocol, s.basedir, "
				+ "  s.typeid, t.name, t.lineends, t.pathformat, "
				+ "  s.credid, c.name, "
				+ "  uc.id, uc.name, uc.realname, s.created, "
				+ "  um.id, um.name, um.realname, s.modified, "
				+ "  uo.id, uo.name, uo.realname, g.id, g.name, c.domainid, "
				+ "	 s.sshport "
				+ "FROM dm.dm_server s "
				+ "LEFT OUTER JOIN dm.dm_serverstatus ss ON s.id = ss.serverid " 	// server status
				+ "LEFT OUTER JOIN dm.dm_servertype t ON s.typeid = t.id "			// server type
				+ "LEFT OUTER JOIN dm.dm_credentials c ON s.credid = c.id "			// credential
				+ "LEFT OUTER JOIN dm.dm_user uc ON s.creatorid = uc.id "			// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON s.modifierid = um.id "			// modifier
				+ "LEFT OUTER JOIN dm.dm_user uo ON s.ownerid = uo.id "				// owner user
				+ "LEFT OUTER JOIN dm.dm_usergroup g ON s.ogrpid = g.id "			// owner group
				+ "WHERE s.id = ?";	
		} else {
			sql = "SELECT s.name, s.summary, s.domainid, s.status "
				+ "FROM dm.dm_server s WHERE s.id = ?";	
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, serverid);
			ResultSet rs = stmt.executeQuery();
			Server ret = null;
			if(rs.next()) {
				ret = new Server(this, serverid, rs.getString(1));
				ret.setSummary(rs.getString(2));
				ret.setDomainId(getInteger(rs, 3, 0));
				getStatus(rs, 4, ret);
				if(detailed) {
					ret.setHostName(rs.getString(5));
					ret.setProtocol(rs.getString(6));
					ret.setBaseDir(rs.getString(7));
					ret.setServerType(new ServerType(
						rs.getInt(8), rs.getString(9),
						LineEndFormat.fromInt(rs.getInt(10)),
						rs.getString(11)));
					int credid = getInteger(rs, 12, 0);
					if(credid != 0) {
						ret.setCredential(new Credential(this, credid, rs.getString(13),rs.getInt(27)));
					}
					getCreatorModifierOwner(rs, 14, ret);
					/*
					String ap = rs.getString(28);
					String amd5 = rs.getString(29);
					ret.setAutoPing(ap!=null?ap.equalsIgnoreCase("y"):false);
					ret.setAutoMD5(amd5!=null?amd5.equalsIgnoreCase("y"):false);
					ret.setPingInterval(rs.getInt(30));
					ret.setPingStart(rs.getInt(31));
					ret.setPingEnd(rs.getInt(32));
					int pingtid = getInteger(rs,33,0);
					int md5tid = getInteger(rs,34,0);
					*/
					int sshport = getInteger(rs,28,22);
					ret.setSSHPort(sshport);
					/*
					NotifyTemplate pingt = pingtid>0?getTemplate(pingtid):null;
					NotifyTemplate md5t = md5tid>0?getTemplate(md5tid):null;
					ret.setPingTemplate(pingt);
					ret.setMD5Template(md5t);
					*/
					ret.setServerCompType(getServerCompType(serverid));
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve server " + serverid + " from database");				
	}
	
	
	public boolean updateServer(Server srv, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_server ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
		String servercomptype = "";
		
		ServerType st = srv.getServerType();
		
		boolean basedirChanged=false;
		boolean serverTypeChanged=false;
		boolean serverProtocolChanged=false;
		int portnum=0;
		
		for(SummaryField field : changes) {
			System.out.println("field = " + field.toString());
			switch(field) {
			case SERVER_TYPE: {
				ServerType type = (ServerType) changes.get(field);
				update.add(", typeid = ?", (type != null) ? type.getId() : Null.INT);
				if (type.getId() != st.getId()) {
					// Server type has changed
					st = type;
					serverTypeChanged=true;
				}
				}
				break;
			case SERVER_HOSTNAME:
				update.add(", hostname = ?", changes.get(field));
				break;
			case SERVER_PROTOCOL:
				update.add(", protocol = ?", changes.get(field));
				serverProtocolChanged=true;
				break;
			case SERVER_BASEDIR:
				String basedir = (String)changes.get(field);
				if (st.getPathFormat().equalsIgnoreCase("windows")) {
					// Ensure slashes are the right way round for windows
					basedir = basedir.replace('/', '\\');
				} else {
					// Ensure slashes are the right way round for Unix/Linux etc
					basedir = basedir.replace('\\', '/');
				}
				basedirChanged=true;
				update.add(", basedir = ?", basedir);
				break;
			case SERVER_CRED: {
				DMObject cred = (DMObject) changes.get(field);
				update.add(", credid = ?", (cred != null) ? cred.getId() : Null.INT);
				}
				break;
			case SERVER_COMPTYPE:
				System.out.println("servercomptype, field = "+changes.get(field));
				servercomptype = (String)changes.get(field);
				break;
			case SERVER_SSHPORT:
				portnum = (Integer.parseInt((String)changes.get(field)));
				update.add(", sshport = ?", portnum);
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(srv,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		if (serverTypeChanged && !basedirChanged) {
			// Server type has changed but not the base directory - base directory may
			// need to change to reflect the new server type.
			String basedir = srv.getBaseDir();
			if (basedir != null) {
				if (st.getPathFormat().equalsIgnoreCase("windows")) {
					// Ensure slashes are the right way round for windows
					basedir = basedir.replace('/', '\\');
				} else {
					// Ensure slashes are the right way round for Unix/Linux etc
					basedir = basedir.replace('\\', '/');
				}
				update.add(", basedir = ?", basedir);
			}
		}
		
		if (serverTypeChanged && !serverProtocolChanged) {
			// Server type has changed but not the protocol. If the server type is NOT windows,
			// make sure the protocol is not windows.
			System.out.println("server type is "+st.getName());
			if (!(st.getName().equalsIgnoreCase("windows"))) {
				// Not windows - if protocol is windows, change it
				System.out.println("srv.getProtocol()="+srv.getProtocol());
				if (srv.getProtocol() == null) {
					update.add(", protocol = ?", "sftp");
				} else {
					if (srv.getProtocol().equalsIgnoreCase("win")) {
						update.add(", protocol = ?", "sftp");
					}
				}
			}
		}
		
		update.add(" WHERE id = ?", srv.getId());
		
		try {
			System.out.println("Updating main table");
			update.execute();
			
			if (servercomptype.length() > 0)
			{
				if (!servercomptype.contains(";")) servercomptype += ";";
			 
				String parts[] = servercomptype.split(";");
				if (parts != null) { 
					String dsql = "DELETE from dm.dm_servercomptype where serverid = ?";
					PreparedStatement dstmt = getDBConnection().prepareStatement(dsql);
					dstmt.setInt(1, srv.getId());
					dstmt.execute();
					dstmt.close();    
     
					for (int k=0;k<parts.length;k++) {
						String tmp = "";
			   
						if (parts[k].length() > 2)
							tmp = parts[k].substring(2);
						else
							tmp = parts[k];
			   
						String ins = "insert into dm.dm_servercomptype(comptypeid,serverid) values (?,?)";
						PreparedStatement insstmt = getDBConnection().prepareStatement(ins);
						insstmt.setInt(1, new Integer(tmp).intValue());
						insstmt.setInt(2, srv.getId());
						insstmt.execute();
						insstmt.close();
					}
				}
			}
			RecordObjectUpdate(srv,changes);
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
 public boolean updateCompType(CompType comptype, SummaryChangeSet changes)
 {
  DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_type ");

  update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_type ");
  update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());

  for (SummaryField field : changes)
  {
   System.out.println("field = " + field.toString());
   switch (field)
   {
    case DATABASE:
     update.add(", database = ?", changes.getBoolean(field));
     break;
    case DELETEDIR:
     update.add(", deletedir = ?", changes.getBoolean(field));
     break;
    case NAME:
     update.add(", name = ?", changes.get(field));
     break;
    default:
     break;
   }
  }

  update.add(" WHERE id = ?", comptype.getId());

  try
  {
   System.out.println("Updating main table");
   update.execute();

   getDBConnection().commit();
   update.close();
   return true;
  }
  catch (SQLException e)
  {
   e.printStackTrace();
   rollback();
  }
  return false;
 }
 	
	
	public boolean updateTemplate(NotifyTemplate t, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_template ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(t,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", t.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateBuildJob(BuildJob t, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_buildjob ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case PROJECTNAME: update.add(", projectname = ?", (String) changes.get(field)); break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(t,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", t.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public ServerType getServerType(int typeid)
	{
		String sql = "SELECT t.name, t.lineends, t.pathformat FROM dm.dm_servertype t WHERE t.id = ?";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, typeid);
			ResultSet rs = stmt.executeQuery();
			ServerType ret = null;
			if(rs.next()) {
				ret = new ServerType(typeid, rs.getString(1), LineEndFormat.fromInt(rs.getInt(2)), getString(rs, 3, null));
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve End Point Type " + typeid + " from database");				
	}
	
	
	public List<ServerType> getServerTypes()
	{
		String sql = "SELECT t.id, t.name, t.lineends, t.pathformat FROM dm.dm_servertype t ORDER BY 2";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			List<ServerType> ret = new ArrayList<ServerType>();
			while(rs.next()) {
				ret.add(new ServerType(rs.getInt(1), rs.getString(2), LineEndFormat.fromInt(rs.getInt(3)), getString(rs, 4, null)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve End Point Types from database");				
	}
	
	public ServerStatus getServerStatus(int serverid)
	{
		ServerStatus ret = null;
		String sql = "SELECT nameresolution,ping,connection,basedir,ipaddr,pingtime,lasterror,lasttime "
					+	"FROM dm.dm_serverstatus WHERE serverid=?";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, serverid);
			ResultSet rs = stmt.executeQuery();
			if(rs.next()) {
				ret = new ServerStatus();
				String NameResolution = rs.getString(1);
				String PingStatus = rs.getString(2);
				String ConnectionStatus = rs.getString(3);
				String BaseDirStatus = rs.getString(4);
				ret.setNameResolution(NameResolution != null?NameResolution.equalsIgnoreCase("y")?"Okay":"Failed":"");
				ret.setPingStatus(PingStatus != null?PingStatus.equalsIgnoreCase("y")?"Okay":"Failed":"");
				ret.setConnectionStatus(ConnectionStatus != null?ConnectionStatus.equalsIgnoreCase("y")?"Okay":"Failed":"");
				ret.setBaseDirStatus(BaseDirStatus != null?BaseDirStatus.equalsIgnoreCase("y")?"Okay":"Failed":"");
				ret.setIPAddress(rs.getString(5));
				ret.setPingTime(rs.getInt(6));
				ret.setLastError(rs.getString(7));
				ret.setLastTime(rs.getInt(8));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve server status date for server " + serverid + " from database");	
	}
	
 public ArrayList<CompType> getServerCompType(int serverid)
 {
  ArrayList<CompType> comptype = new ArrayList<CompType>();
  
  String sql = "SELECT DISTINCT a.id, a.name, a.database, a.deletedir, a.domainid from dm.dm_type a, dm.dm_servercomptype b WHERE b.serverid=? and b.comptypeid = a.id";
  
  try
  {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, serverid);
   ResultSet rs = stmt.executeQuery();
   while(rs.next()) {
    CompType t = new CompType(this, rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5));
    comptype.add(t);
   }
   rs.close();
   stmt.close();
   return comptype;
  }
  catch(SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  throw new RuntimeException("Unable to retrieve End Point component type for " + serverid + " from database"); 
 }
 
 public List<CompType> getCompTypes()
 {
  String sql = "SELECT id, name, database, deletedir, domainid FROM dm.dm_type t ORDER BY 2";
  
  try
  {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   ResultSet rs = stmt.executeQuery();
   List<CompType> ret = new ArrayList<CompType>();
   while(rs.next()) {
	   CompType ct = new CompType(this, rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),rs.getInt(5));
	   System.out.println("got component type "+ct.getName());
	   if (ct.isViewable(true)) {
		   System.out.println("isViewable returns true");
		   ret.add(ct);
	   } else System.out.println("isViewable returns false");
   }
   rs.close();
   stmt.close();
   return ret;
  }
  catch(SQLException ex)
  {
   ex.printStackTrace();
  }
  throw new RuntimeException("Unable to retrieve End Point Types from database");    
 }
 
	
	public Task getTask(int taskid, boolean detailed)
	{
		String sql = null;
		if(detailed) {
			sql = "SELECT t.name, t.domainid, tt.name, t.logoutput, t.subdomains, t.successtemplateid, t.failuretemplateid, "
				+ "  uc.id, uc.name, uc.realname, t.created, "
				+ "  um.id, um.name, um.realname, t.modified, "
				+ "  a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
				+ "FROM dm.dm_task t "
				+ "LEFT OUTER JOIN dm.dm_tasktypes tt ON t.typeid = tt.id "		// task type
				+ "LEFT OUTER JOIN dm.dm_user uc ON t.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON t.modifierid = um.id "		// modifier
				+ "LEFT OUTER JOIN dm.dm_action a1 ON t.preactionid = a1.id "	// pre-action
				+ "LEFT OUTER JOIN dm.dm_action a2 ON t.postactionid = a2.id "	// post-action
				+ "WHERE t.id = ?";	
		} else {
			sql = "SELECT t.name, t.domainid, tt.name FROM dm.dm_task t, dm.dm_tasktypes tt WHERE t.id = ? AND t.typeid = tt.id";	
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, taskid);
			ResultSet rs = stmt.executeQuery();
			Task ret = null;
			if(rs.next()) {
				ret = new Task(this, taskid, rs.getString(1));
				ret.setDomainId(getInteger(rs,2,0));
				ret.setTaskType(rs.getString(3));
				if(detailed) {
					System.out.println("id="+taskid+" subdomains="+getBoolean(rs,5,false));
					ret.setShowOutput(getBoolean(rs, 4, false));
					ret.setSubDomains(getBoolean(rs, 5, false));
					int stid = getInteger(rs,6,0);
					int ftid = getInteger(rs,7,0);
					System.out.println("stid="+stid+" ftid="+ftid);
					NotifyTemplate nt = stid>0?getTemplate(stid):null;
					NotifyTemplate rt = ftid>0?getTemplate(ftid):null;
					ret.setSuccessTemplate(nt);
					ret.setFailureTemplate(rt);
					getCreatorModifier(rs, 8, ret);
					getPreAndPostActions(rs, 16, ret);
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve task " + taskid + " from database");				
	}
	
	
	public ReportDataSet getDeploymentsPerApplicationForServer(int serverid)
	{
		String sql = "SELECT a.name, ("
			+ "SELECT count(*) FROM dm.dm_deployment d WHERE d.appid = a.id AND d.envid=sie.envid AND d.exitcode = 0"
			+ ") as success, ("
			+ "SELECT count(*) FROM dm.dm_deployment d WHERE d.appid = a.id AND d.envid=sie.envid AND d.exitcode <> 0"
			+ ") as failed "
			+ "FROM dm.dm_application a, dm.dm_appsallowedinenv aie, dm.dm_serversinenv sie WHERE a.id = aie.appid AND sie.envid = aie.envid AND sie.serverid=? "
			+ "ORDER BY a.name";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, serverid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs);
			rs.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve application deployments report from database");		
	}
	
	// Object
	
	private String getDeploymentQuery(boolean hasApp, boolean hasEnv, String from, String where)
	{
		// TODO: Check the subscribed logic - surely needs to include env as well
		/*
		return "SELECT 'de', d.started, d.deploymentid, d.exitcode, "
			+ (hasApp ? "d.appid, a.name" : "0, ''") + ", "
			+ (hasEnv ? "d.envid, e.name" : "0, ''") + ", "
			+ "d.userid, u.name, u.realname, d.eventid||'', '', "
		    + "  (SELECT max(z.startwhen) FROM ("
			+ "    SELECT d2.started AS startwhen FROM dm.dm_deployment d2 WHERE d2.deploymentid=d.deploymentid "
		    + "    UNION "
			+ "    SELECT c2.when AS startwhen FROM dm.dm_historycomment c2 WHERE c2.id = d.deploymentid AND c2.kind = " + ObjectType.DEPLOYMENT.value() + ") AS z), "
			+ "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = d.deploymentid AND s.kind = " + ObjectType.DEPLOYMENT.value() + "), "
			+ "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = d.deploymentid AND c.kind = " + ObjectType.DEPLOYMENT.value() + "), "
			+ "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = d.deploymentid AND a.kind = " + ObjectType.DEPLOYMENT.value() + ") "
			+ "FROM dm.dm_deployment d, "
			+ (hasApp ? "dm.dm_application a, " : "")
			+ (hasEnv ? "dm.dm_environment e, " : "")
			+ ((from != null) ? (from + ", ") : "")
			+ "dm.dm_user u WHERE " + where + " "
			+ (hasApp ? "AND d.appid = a.id " : "")
			+ (hasEnv ? "AND d.envid = e.id " : "")
			+ "AND d.userid = u.id ";
			*/
		// Reworked to be compatible with Postgres and Oracle
		return "SELECT 'de', d.started, d.deploymentid, d.exitcode, "
		+ (hasApp ? "d.appid, a.name" : "0, ''") + ", "
		+ (hasEnv ? "d.envid, e.name" : "0, ''") + ", "
		+ "d.userid, u.name, u.realname, d.eventid||'', '', "
		+ "(	"
		+ "		SELECT	GREATEST(max(d2.started),max(c2."+whencol+"))	"
		+ "		FROM	dm.dm_deployment d2,	"
		+ "				dm.dm_historycomment c2		"
		+ "		WHERE	d2.deploymentid=d.deploymentid	"
		+ "		AND		c2.id = d.deploymentid	"
		+ "		AND		c2.kind = " +	ObjectType.DEPLOYMENT.value()
		+ "	),	"
		+ "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = d.deploymentid AND s.kind = " + ObjectType.DEPLOYMENT.value() + "), "
		+ "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = d.deploymentid AND c.kind = " + ObjectType.DEPLOYMENT.value() + "), "
		+ "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = d.deploymentid AND a.kind = " + ObjectType.DEPLOYMENT.value() + ") "
		+ "FROM dm.dm_deployment d, "
		+ (hasApp ? "dm.dm_application a, " : "")
		+ (hasEnv ? "dm.dm_environment e, " : "")
		+ ((from != null) ? (from + ", ") : "")
		+ "dm.dm_user u WHERE " + where + " "
		+ (hasApp ? "AND d.appid = a.id " : "")
		+ (hasEnv ? "AND d.envid = e.id " : "")
		+ "AND d.userid = u.id ";
	}

	private String getApprovalQuery(boolean calcSubs, String from, String where)
	{
		/*
		return "SELECT 'ae', n.when, n.id, " + ObjectType.APPLICATION.value() + ", "
			 + "n.appid, a2.name,  n.domainid, ad.name, "
			 + "n.userid, u.name, u.realname, n.note, n.approved, "
			 + "  (SELECT max(y.startwhen) FROM ("
			 + "    SELECT n2.when AS startwhen FROM dm.dm_approval n2 WHERE n2.id = n.id "
			 + "    UNION "
			 + "    SELECT c2.when AS startwhen FROM dm.dm_historycomment c2 WHERE c2.id = n.id AND c2.kind = " + ObjectType.APPROVAL.value() + ") AS y), "
			 + (calcSubs ? "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = n.appid AND s.kind = " + ObjectType.APPLICATION.value() + "), " : "1, ")
			 + "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = n.id AND c.kind = " + ObjectType.APPROVAL.value() + "), "
			 + "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = n.id AND a.kind = " + ObjectType.APPROVAL.value() + ") "
			 + "FROM dm.dm_approval n, dm.dm_application a2, dm.dm_domain ad, "
			 + ((from != null) ? (from + ", ") : "")
			 + "dm.dm_user u "
			 + "WHERE " + where + " "
			 + "AND n.appid = a2.id AND n.domainid = ad.id AND n.userid = u.id ";
		 */
		// Reworked to be compatible with Postgres and Oracle
		return "SELECT 'ae', n."+whencol+", n.id, " + ObjectType.APPLICATION.value() + ", "
		 + "n.appid, a2.name,  n.domainid, ad.name, "
		 + "n.userid, u.name, u.realname, n.note, n.approved, "
		 + "(	"
		 + "	SELECT	GREATEST(max(n2."+whencol+"),max(c2."+whencol+"))	"
		 + "	FROM	dm.dm_approval			n2,	"
		 + "			dm.dm_historycomment	c2	"
		 + "	WHERE 	n2.id = n.id	"
		 + "	AND		c2.id = n.id	"
		 + "	AND		c2.kind =" + ObjectType.APPROVAL.value()
		 + "),	"
		 + (calcSubs ? "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = n.appid AND s.kind = " + ObjectType.APPLICATION.value() + "), " : "1, ")
		 + "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = n.id AND c.kind = " + ObjectType.APPROVAL.value() + "), "
		 + "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = n.id AND a.kind = " + ObjectType.APPROVAL.value() + ") "
		 + "FROM dm.dm_approval n, dm.dm_application a2, dm.dm_domain ad, "
		 + ((from != null) ? (from + ", ") : "")
		 + "dm.dm_user u "
		 + "WHERE " + where + " "
		 + "AND n.appid = a2.id AND n.domainid = ad.id AND n.userid = u.id ";
		
	}
	
	private String getRequestQuery(boolean calcSubs, String from, String where)
	{
		/*
		return "SELECT 'rq', n.when, n.id, " + ObjectType.APPLICATION.value() + ", "
			 + "n.appid, a2.name, t.id, t.name, "
			 + "n.userid, u.name, u.realname, n.note, '', "
			 + "  (SELECT max(y.startwhen) FROM ("
			 + "    SELECT n2.when AS startwhen FROM dm.dm_request n2 WHERE n2.id = n.id "
			 + "    UNION "
			 + "    SELECT c2.when AS startwhen FROM dm.dm_historycomment c2 WHERE c2.id = n.id AND c2.kind = " + ObjectType.REQUEST.value() + ") AS y), "
			 + (calcSubs ? "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = n.appid AND s.kind = " + ObjectType.APPLICATION.value() + "), " : "1, ")
			 + "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = n.id AND c.kind = " + ObjectType.REQUEST.value() + "), "
			 + "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = n.id AND a.kind = " + ObjectType.REQUEST.value() + ") "
			 + "FROM dm.dm_request n, dm.dm_application a2, dm.dm_task t, "
			 + ((from != null) ? (from + ", ") : "")
			 + "dm.dm_user u "
			 + "WHERE " + where + " "
			 + "AND n.appid = a2.id AND n.userid = u.id and t.id = n.taskid ";
			 */
		// Reworked to be compatible with Postgres and Oracle
		return "SELECT 'rq', n."+whencol+", n.id, " + ObjectType.APPLICATION.value() + ", "
		 + "n.appid, a2.name, t.id, t.name, "
		 + "n.userid, u.name, u.realname, n.note, '', "
		 + "(	"
		 + "	SELECT	GREATEST(max(n2."+whencol+"),max(c2."+whencol+"))	"
		 + "	FROM	dm.dm_request 			n2,	"
		 + "			dm.dm_historycomment	c2	"
		 + "	WHERE	n2.id = n.id	"
		 + "	AND		c2.id = n.id	"
		 + "	AND		c2.kind = " + ObjectType.REQUEST.value()
		 + "),	"
		 + (calcSubs ? "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = n.appid AND s.kind = " + ObjectType.APPLICATION.value() + "), " : "1, ")
		 + "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = n.id AND c.kind = " + ObjectType.REQUEST.value() + "), "
		 + "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = n.id AND a.kind = " + ObjectType.REQUEST.value() + ") "
		 + "FROM dm.dm_request n, dm.dm_application a2, dm.dm_task t, "
		 + ((from != null) ? (from + ", ") : "")
		 + "dm.dm_user u "
		 + "WHERE " + where + " "
		 + "AND n.appid = a2.id AND n.userid = u.id and t.id = n.taskid ";
	}
	
	private String getNotesQuery(String objCol, String objCol2, boolean calcSubs, String from, String where)
	{
		System.out.println("getNotesQuery where="+where);
		/*
		return "SELECT 'hn', n.when, n.id, n.kind, n.objid, "
			 + ((objCol != null) ? objCol : "''") + ", "
			 + "0, "
			 + ((objCol2 != null) ? objCol2 : "''") + ", "
			 + "n.userid, u.name, u.realname, n.note, n.icon, "
			 + "  (SELECT max(y.startwhen) FROM ("
			 + "    SELECT n2.when AS startwhen FROM dm.dm_historynote n2 WHERE n2.id = n.id "
			 + "    UNION "
			 + "    SELECT c2.when AS startwhen FROM dm.dm_historycomment c2 WHERE c2.id = n.id AND c2.kind = " + ObjectType.HISTORY_NOTE.value() + ") AS y), "
			 + (calcSubs ? "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = n.objid AND s.kind = n.kind), " : "1, ")
			 + "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = n.id AND c.kind = " + ObjectType.HISTORY_NOTE.value() + "), "
			 + "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = n.id AND a.kind = " + ObjectType.HISTORY_NOTE.value() + ") "
			 + "FROM dm.dm_historynote n, "
			 + ((from != null) ? (from + ", ") : "")
			 + "dm.dm_user u "
			 + "WHERE " + where + " "
			 + "AND n.userid = u.id ";
			 */
		// Reworked to be compatible with Postgres and Oracle
		return "SELECT 'hn', n."+whencol+", n.id, n.kind, n.objid, "
		 + ((objCol != null) ? objCol : "''") + ", "
		 + "0, "
		 + ((objCol2 != null) ? objCol2 : "''") + ", "
		 + "n.userid, u.name, u.realname, n.note, n.icon, "
		 + "(	"
		 + "	SELECT	GREATEST(max(n2."+whencol+"),max(c2."+whencol+"))	"
		 + "	FROM	dm.dm_historynote 	n2,	"
		 + "			dm.dm_historycomment	c2	"
		 + "	WHERE	n2.id = n.id	"
		 + "	AND		c2.id = n.id	"
		 + "	AND		c2.kind = " + ObjectType.HISTORY_NOTE.value()
		 + "),	"
		 + (calcSubs ? "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = n.objid AND s.kind = n.kind), " : "1, ")
		 + "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = n.id AND c.kind = " + ObjectType.HISTORY_NOTE.value() + "), "
		 + "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = n.id AND a.kind = " + ObjectType.HISTORY_NOTE.value() + ") "
		 + "FROM dm.dm_historynote n, "
		 + ((from != null) ? (from + ", ") : "")
		 + "dm.dm_user u "
		 + "WHERE " + where + " "
		 + "AND n.userid = u.id ";
	}
	
	private String getCreationQuery(ObjectTypeAndId otid, String table, String icon, String text, boolean calcSubs, String from, String where)
	{
		int kind  = otid.getObjectType().value();
		return "SELECT 'no', o.created, 0, " + kind + ", o.id, o.name, 0, '', "
			+ "o.creatorid, u.name, u.realname, '" + text + "', '" + icon + "', o.created, "
			 + (calcSubs ? "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = o.id AND s.kind = " + kind + "), " : "1, ")
			 + "0, 0 "
			 + "FROM dm.dm_" + table + " o, "
			 + ((from != null) ? (from + ", ") : "")
			 + "dm.dm_user u "
			 + "WHERE " + where + " AND o.created > 0 AND o.creatorid = u.id ";
	}
	
	private String getBuildQuery(int compid)
	{
		if (dbdriver.toLowerCase().contains("oracle")) {
			return "SELECT 'bu', a.timestamp, a.buildnumber, " + ObjectType.BUILDJOB.value() + ", a.buildnumber, '', a.compid, '', "
					+ "a.userid, u.name, u.realname, 'Build', decode(a.success,'Y','blue','red'), a.timestamp, "
					+ " 0, "
					+ "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = a.compid AND c.kind = " + ObjectType.BUILDID.value() + " AND c.subid=a.buildnumber), "
					+ " 0"
					+ "FROM dm.dm_buildhistory	a,	"
					+ "     dm.dm_user u "
					+ "WHERE a.compid="+compid+" "
					+" AND u.id=a.userid";
		} else {
			return "SELECT 'bu', a.timestamp, a.buildnumber, " + ObjectType.BUILDJOB.value() + ", a.buildnumber, '', a.compid, '', "
					+ "a.userid, u.name, u.realname, 'Build', (CASE WHEN a.success='Y' THEN 'blue' ELSE 'red' END), a.timestamp, "
					+ " 0, "
					+ "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = a.compid AND c.kind = " + ObjectType.BUILDID.value() + " AND c.subid=a.buildnumber), "
					+ " 0"	// attachment count
					+ "FROM dm.dm_buildhistory	a,	"
					+ "     dm.dm_user u "
					+ "WHERE a.compid="+compid+" "
					+" AND u.id=a.userid";
		}
	}
	
	private String getCreatedObjectsQuery(int userid)
	{
		// Environments
		return "SELECT 'no', o.created, 0, " + ObjectType.ENVIRONMENT.value() + ", o.id, o.name, 0, '', "
				+ "o.creatorid, u.name, u.realname, '" + "Created" + "', '" + "environment" + "', o.created, "
				 + "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = o.id AND s.kind = " + ObjectType.ENVIRONMENT.value() + "), "
				 + "0, 0 "
				 + "FROM dm.dm_environment o, "
				 + "dm.dm_user u "
				 + "WHERE o.status='N' AND o.creatorid="+userid+" AND o.created > 0 AND o.creatorid = u.id "
				 + "UNION "
				 // Servers
				 + "SELECT 'no', o.created, 0, " + ObjectType.SERVER.value() + ", o.id, o.name, 0, '', "
				 + "o.creatorid, u.name, u.realname, '" + "Created" + "', '" + "server" + "', o.created, "
				 + "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = o.id AND s.kind = " + ObjectType.SERVER.value() + "), "
				 + "0, 0 "
				 + "FROM dm.dm_server o, "
				 + "dm.dm_user u "
				 + "WHERE o.status='N' AND o.creatorid="+userid+" AND o.created > 0 AND o.creatorid = u.id "
				 + "UNION "
				 // BASE Applications
				 + "SELECT 'no', o.created, 0, " + ObjectType.APPLICATION.value() + ", o.id, o.name, 0, '', "
				 + "o.creatorid, u.name, u.realname, '" + "Created" + "', '" + "application" + "', o.created, "
				 + "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = o.id AND s.kind = " + ObjectType.APPLICATION.value() + "), "
				 + "0, 0 "
				 + "FROM dm.dm_application o, "
				 + "dm.dm_user u "
				 + "WHERE o.status='N' AND o.creatorid="+userid+" AND o.parentid is null and o.isrelease='N' and o.created > 0 AND o.creatorid = u.id "
				 + "UNION "
				 // BASE Components
				 + "SELECT 'no', o.created, 0, " + ObjectType.COMPONENT.value() + ", o.id, o.name, 0, '', "
				 + "o.creatorid, u.name, u.realname, '" + "Created" + "', '" + "component" + "', o.created, "
				 + "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = o.id AND s.kind = " + ObjectType.COMPONENT.value() + "), "
				 + "0, 0 "
				 + "FROM dm.dm_component o, "
				 + "dm.dm_user u "
				 + "WHERE o.status='N' AND o.creatorid="+userid+" AND o.parentid is null and o.created > 0 AND o.creatorid = u.id "
				 + "UNION "
				 // Application Versions
				 + "SELECT 'no', o.created, 0, " + ObjectType.APPVERSION.value() + ", o.id, o.name, 0, '', "
				 + "o.creatorid, u.name, u.realname, '" + "Created" + "', '" + "appversion" + "', o.created, "
				 + "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = o.id AND s.kind = " + ObjectType.APPVERSION.value() + "), "
				 + "0, 0 "
				 + "FROM dm.dm_application o, "
				 + "dm.dm_user u "
				 + "WHERE o.status='N' AND o.creatorid="+userid+" AND o.parentid is not null and o.isrelease='N' and o.created > 0 AND o.creatorid = u.id "
				 + "UNION "
				 // Component Versions
				 + "SELECT 'no', o.created, 0, " + ObjectType.COMPONENT.value() + ", o.id, o.name, 0, '', "
				 + "o.creatorid, u.name, u.realname, '" + "Created" + "', '" + "component_ver" + "', o.created, "
				 + "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = o.id AND s.kind = " + ObjectType.COMPONENT.value() + "), "
				 + "0, 0 "
				 + "FROM dm.dm_component o, "
				 + "dm.dm_user u "
				 + "WHERE o.status='N' AND o.creatorid="+userid+" AND o.parentid is not null and o.created > 0 AND o.creatorid = u.id "
				 ;
	}
	
	
	public NewsFeedDataSet getNewsForObject(ObjectTypeAndId otid)
	{
		return getHistoryNewsForObject(otid, 0, 0);
	}
	
	public NewsFeedDataSet getHistoryNewsForObject(ObjectTypeAndId otid, int from, int to)
	{
		boolean isObjectNewsFeed = (otid != null);
		DynamicQueryBuilder query = new DynamicQueryBuilder(getDBConnection(), "");
		
		System.out.println("getHistoryNewsForObject");
				
		String sinceClause1 = "";
		String sinceClause2 = "";
		String sinceClause3 = "";
		if(from != 0) {
			sinceClause1 += "AND d.started > " + from + " ";
			sinceClause2 += "AND n."+whencol+" > " + from + " ";
			sinceClause3 += "AND o.created > " + from + " ";
		}
		if(to != 0) {
			sinceClause1 += "AND d.started < " + to + " ";
			sinceClause2 += "AND n."+whencol+" < " + to + " ";
			sinceClause3 += "AND o.created < " + to + " ";
		}
		
		/**
		 * Columns are:
		 *     NAME     DEPLOYMENT           NOTE
		 *  1: kind  - 'd' for deployment    'n' for note
		 *  2: when  - seconds since 1/1/1970 when deployment took place or when note was added
		 *  3: id    - deploymentid          noteid
		 *  4:         exitcode              objtype
		 *  5:         not used or appid     objid
		 *  6:         not used or appname   not used or objname
		 *  7:         not used or envid     not used
		 *  8:         not used or envname   not used
		 *  9: userid
		 * 10: username
		 * 11: userrealname
		 * 12:         eventid (auto deploy) text
		 * 13:         not used              icon
		 * 14: sortwhen   - the max between the item and its comments
		 * 15: subscribed - subscription count
		 * 16: cmntcnt    - comment count
		 * 17: attachcnt  - attachment count
		 */
		
		// DEPLOYMENTS
		if(isObjectNewsFeed) {
			switch(otid.getObjectType()) {
			case ENVIRONMENT:
				System.out.println("History for ENVIRONMENT");
				query.add(getDeploymentQuery(true, false, null, "d.envid=?") + sinceClause1, otid.getId());
				break;
			case APPLICATION:
			case RELEASE: 
			case RELVERSION:    
			case APPVERSION:				 
				query.add(getDeploymentQuery(false, true, null, "d.appid=?") + sinceClause1, otid.getId());
				//TODO: Add deployments of child appvers for application - needs result code changing to handle
				//      query.add("UNION " + getDeploymentQuery(true, true, null, "a.parentid=?") + sinceClause1, otid.getId());
				break;
			case COMPONENT:    
			case COMPVERSION:
				query.add(getDeploymentQuery(false, true, null, "d.deploymentid in (select distinct x.deploymentid FROM dm.dm_deploymentxfer x where x.componentid=?)") + sinceClause1, otid.getId());		
				break;
			case SERVER:
				query.add(getDeploymentQuery(true, true, "dm.dm_deploymentxfer x", "x.serverid=? AND d.deploymentid = x.deploymentid") + sinceClause1, otid.getId());
				break;
			case REPOSITORY:
				query.add(getDeploymentQuery(true, true, "dm.dm_deploymentxfer x", "x.repoid=? AND d.deploymentid = x.deploymentid") + sinceClause1, otid.getId());
				break;
			case USER:
				query.add(getDeploymentQuery(true, true, null, "d.userid=?") + sinceClause1, otid.getId());
				break;
			case PROCEDURE:
			case FUNCTION:
			case ACTION:
				query.add(getDeploymentQuery(true, true, "dm.dm_deploymentactions x", "x.actionid=? AND d.deploymentid=x.deploymentid") + sinceClause1, otid.getId());
				break;
			case USERGROUP:
				// A user group doesn't really have anything to do with deployment but we need SOMETHING
				// for the queries below to UNION to.. so do a dummy here
				query.add(getDeploymentQuery(false,false,null,"d.userid=-1") +sinceClause1);
				break;
			default:
				throw new RuntimeException("Unable to retrieve history for " + otid.getObjectType() + " from database");
			}
		} else {
			// Show only subscribed items
			// TODO: Add deployments involving a repository - need to look in deploymentxfer for this
			query.add(getDeploymentQuery(true, true, "dm.dm_historysubs s",
				"((d.appid = s.id AND s.kind = " + ObjectType.APPLICATION.value() + ")  "
				+ " OR (d.envid = s.id AND s.kind = " + ObjectType.ENVIRONMENT.value() + ") "
				+ " OR (d.deploymentid = s.id AND s.kind = " + ObjectType.DEPLOYMENT.value() + ") "
				+ " OR (d.userid = s.id AND s.kind = " + ObjectType.USER.value() + ")) AND s.userid = ? ") + sinceClause1, getUserID());
		}
		
		
		// APPROVALS
		if(isObjectNewsFeed) {
			switch(otid.getObjectType()) {
			case RELEASE:
			case RELVERSION:			 
			case APPLICATION:
			case APPVERSION:
				query.add(" UNION " + getApprovalQuery(true, null, "n.appid=?") + sinceClause2, otid.getId());
				break;
			case USER:
				query.add(" UNION " + getApprovalQuery(true, null, "n.userid=?") + sinceClause2, otid.getId());
				break;
			default: break;
			}
		} else {
			// Show only subscribed items
			query.add(" UNION " + getApprovalQuery(false, "dm.dm_historysubs s",
					"n.appid=s.id AND s.kind = ? AND s.userid = ?") + sinceClause2, ObjectType.APPLICATION.value(), getUserID());
		}
		
		// REQUESTS
		if (isObjectNewsFeed) {
			switch(otid.getObjectType()) {
			case RELEASE:
			case RELVERSION:  			 
			case APPLICATION:
			case APPVERSION:
				query.add("UNION " + getRequestQuery(true, null, "n.appid=?") + sinceClause2, otid.getId());
				break;
			case USER:
				query.add("UNION " + getRequestQuery(true, null, "n.userid=?") + sinceClause2, otid.getId());
				break;
			default: break;
			}
		} else {
			// Show only subscribed items
			query.add(" UNION " + getRequestQuery(false, "dm.dm_historysubs s",
					"n.appid=s.id AND s.kind = ? AND s.userid = ?") + sinceClause2, ObjectType.APPLICATION.value(), getUserID());
		}
		
		
		// NOTES
		System.out.println("Doing notes, isObjectNewsFeed="+isObjectNewsFeed);
		if(isObjectNewsFeed) {
			// Object itself
			if (otid.getObjectType() == ObjectType.APPLICATION || otid.getObjectType() == ObjectType.APPVERSION) {
				// Bug fix - move domain entries in dm_historynote can have APPLICATION rather than APPVERSION. Take either option for safety
				System.out.println("Taking either app or appversion");
				query.add(" UNION " + getNotesQuery(null, null, true, null, "n.objid=? AND n.kind in (?,?)") + sinceClause2, otid.getId(), ObjectType.APPLICATION.value(),ObjectType.APPVERSION.value());
			} else {
				System.out.println("Taking object type "+otid.getObjectType()+" id1="+otid.getId()+" id2="+otid.getObjectType().value());
				query.add(" UNION " + getNotesQuery(null, null, true, null, "n.objid=? AND n.kind=?") + sinceClause2, otid.getId(), otid.getObjectType().value());
			}
			System.out.println("query is "+query.getQueryString());
			// Related objects - TODO: Make this optional
			switch(otid.getObjectType()) {
			case ENVIRONMENT:
				// Server notes for servers in this environment
				query.add(" UNION " + getNotesQuery("s.name", null, true, "dm.dm_serversinenv sie, dm.dm_server s",
					"COALESCE(n.linkid,0) <> sie.envid AND n.objid = sie.serverid AND sie.envid=? AND n.kind=? AND s.id = sie.serverid") + sinceClause2, otid.getId(), ObjectType.SERVER.value());
				// Applications notes for applications in this environment
				query.add(" UNION " + getNotesQuery("a.name", null, true, "dm.dm_appsallowedinenv aie, dm.dm_application a",
					"COALESCE(n.linkid,0) <> aie.envid AND n.objid = aie.appid AND aie.envid=? AND n.kind=? AND a.id = aie.appid") + sinceClause2, otid.getId(), ObjectType.APPLICATION.value());
				break;
			case RELEASE:
			case RELVERSION:  				
			case APPLICATION:
			case APPVERSION:
				// Environment notes for environments this application can be deployed to
				query.add(" UNION " + getNotesQuery("e.name", null, true, "dm.dm_appsallowedinenv aie, dm.dm_environment e",
						"COALESCE(n.linkid,0) <> aie.appid AND n.objid = aie.envid AND aie.appid=? AND n.kind=? AND e.id = aie.envid") + sinceClause2, otid.getId(), ObjectType.ENVIRONMENT.value());
				// Server notes for servers in environments this application can be deployed to
				query.add(" UNION " + getNotesQuery("s.name", null, true, "dm.dm_appsallowedinenv aie, dm.dm_serversinenv sie, dm.dm_server s",
						"COALESCE(n.linkid,0) <> aie.appid AND n.objid = sie.serverid AND aie.appid=? AND n.kind=? AND sie.envid = aie.envid AND s.id = sie.serverid") + sinceClause2, otid.getId(), ObjectType.SERVER.value());
				break;
			case SERVER:
				// Environment notes for environments this server is a part of
				query.add(" UNION " + getNotesQuery("e.name", null, true, "dm.dm_serversinenv sie, dm.dm_environment e",
						"COALESCE(n.linkid,0) <> sie.serverid AND n.objid = sie.envid AND sie.serverid=? AND n.kind=? AND e.id = sie.envid") + sinceClause2, otid.getId(), ObjectType.ENVIRONMENT.value());
				// Application notes for applications in environments this server is a part of
				query.add(" UNION " + getNotesQuery("a.name", null, true, "dm.dm_serversinenv sie, dm.dm_appsallowedinenv aie, dm.dm_application a",
						"COALESCE(n.linkid,0) <> sie.serverid AND n.objid = aie.appid AND sie.serverid=? AND n.kind=? AND aie.envid = sie.envid AND a.id = aie.appid") + sinceClause2, otid.getId(), ObjectType.APPLICATION.value());
				break;
			case REPOSITORY:
				// Nothing to add for repository
				break;
			case USER:
				// Any notes on applications
				query.add(" UNION " + getNotesQuery("a.name", null, true, "dm.dm_application a",
						"n.userid = ? AND n.kind = ? AND n.objid = a.id") + sinceClause2, otid.getId(), ObjectType.APPLICATION.value());
				// Any notes on environments
				query.add(" UNION " + getNotesQuery("e.name", null, true, "dm.dm_environment e",
						"n.userid = ? AND n.kind = ? AND n.objid = e.id") + sinceClause2, otid.getId(), ObjectType.ENVIRONMENT.value());
				// Any notes on servers
				query.add(" UNION " + getNotesQuery("s.name", null, true, "dm.dm_server s",
						"n.userid = ? AND n.kind = ? AND n.objid = s.id") + sinceClause2, otid.getId(), ObjectType.SERVER.value());
				// Any notes on repositories
				query.add(" UNION " + getNotesQuery("r.name", null, true, "dm.dm_repository r",
						"n.userid = ? AND n.kind = ? AND n.objid = r.id") + sinceClause2, otid.getId(), ObjectType.REPOSITORY.value());
				// Any notes on other users - notes on this user have been done
				query.add(" UNION " + getNotesQuery("u2.name", "u2.realname", true, "dm.dm_user u2",
						"n.userid = ? AND n.kind = ? AND n.objid = u2.id AND NOT n.objid = n.userid") + sinceClause2, otid.getId(), ObjectType.USER.value());
				// TODO: any comments added by this user
				break;
			case USERGROUP:
				break;
			default:
				System.out.println("No notes query added for "+otid.getObjectType());
				break;
			}
		} else {
			// Applications
			query.add(" UNION " + getNotesQuery("a2.name", null, false, "dm.dm_historysubs s, dm.dm_application a2",
					"n.objid=s.id AND n.kind=s.kind AND n.kind = ? AND n.objid = a2.id AND s.userid = ?") + sinceClause2, ObjectType.APPLICATION.value(), getUserID());
			
			// Environments
			query.add(" UNION " + getNotesQuery("e2.name", null, false, "dm.dm_historysubs s, dm.dm_environment e2",
					"n.objid=s.id AND n.kind=s.kind AND n.kind = ? AND n.objid = e2.id AND s.userid = ?") + sinceClause2, ObjectType.ENVIRONMENT.value(), getUserID());
			
			// Other - not application or environment - we won't retrieve the name for this object
			query.add(" UNION " + getNotesQuery(null, null, false, "dm.dm_historysubs s",
				"n.objid=s.id AND n.kind=s.kind AND n.kind NOT IN (" + ObjectType.ENVIRONMENT.value()
				+ "," + ObjectType.APPLICATION.value() + ") AND s.userid = ?") + sinceClause2, getUserID());
		}
		
		// Creation events
		if(isObjectNewsFeed) {
			switch(otid.getObjectType()) {
			case RELEASE:  
			case APPLICATION:
				query.add(" UNION " + getCreationQuery(otid, "application", "newapp", "Created", true,
						null, "o.id = ?") + sinceClause3, otid.getId());
				query.add(" UNION " + getCreationQuery(otid, "application", "newappver", "Created new version", true,
						"dm.dm_application po", "po.id = ? AND o.parentid = po.id") + sinceClause3, otid.getId());
				break;
			case RELVERSION:  				
			case APPVERSION:
				query.add(" UNION " + getCreationQuery(otid, "application", "newappver", "Created", true,
						null, "o.id = ?") + sinceClause3, otid.getId());
				break; 
			case COMPONENT:
				query.add(" UNION " + getCreationQuery(otid, "component", "newcomp", "Created", true,
						null, "o.id = ?") + sinceClause3, otid.getId());
				query.add(" UNION " + getCreationQuery(otid, "component", "newcompver", "Created new version", true,
						"dm.dm_component po", "po.id = ? AND o.parentid = po.id") + sinceClause3, otid.getId());
				break;
			case COMPVERSION:
				query.add(" UNION " + getCreationQuery(otid, "component", "newcompver", "Created", true,
						null, "o.id = ?") + sinceClause3, otid.getId());
				break;				
			case ENVIRONMENT:
				query.add(" UNION " + getCreationQuery(otid, "environment", "environment", "Created", true,
						null, "o.id = ?") + sinceClause3, otid.getId());
				break;
			case SERVER:
				query.add(" UNION " + getCreationQuery(otid, "server", "server", "Created", true,
						null, "o.id = ?") + sinceClause3, otid.getId());
			case USER:
				query.add(" UNION " + getCreationQuery(otid, "user", "user", "Created", true,
						null, "o.id = ?") + sinceClause3, otid.getId());
				query.add(" UNION " + getCreatedObjectsQuery(otid.getId()));
				break;
			case USERGROUP:
				break;
			default:
				break;	
			}			
		} else {
			
		}
		
		// BUILDS
		if(isObjectNewsFeed) {
			switch(otid.getObjectType()) {
			case COMPONENT:
			case COMPVERSION:
				query.add(" UNION " + getBuildQuery(otid.getId()));
				break;
			default:
				break;
			}
		}

		System.out.println("here");
		query.add(" ORDER BY 2, 14");
		
		try
		{
			ResultSet rs = query.executeQuery();
			NewsFeedDataSet ret = new NewsFeedDataSet();
			while(rs.next()) {
				PropertyDataSet item = new PropertyDataSet();
				String mykind = rs.getString(1);
				System.out.println("otid="+otid+"mykind="+mykind);
				item.addProperty("kind", mykind);	// 'de', 'hn' or 'ae'
				int when = rs.getInt(2);
				item.addProperty("when", when);
				item.addProperty("whenstr", formatDateToUserLocale(when));
				item.addProperty("id", rs.getInt(3));
				if(mykind.equals("de")) {
					//item.addProperty("objkind", ObjectType.DEPLOYMENT.value());
					item.addProperty("icon", (rs.getInt(4) == 0) ? "deploy" : "deployfail");
					if((otid == null) || (otid.getObjectType() != ObjectType.APPLICATION)) {
						int itemid = rs.getInt(5);
						Application app = (itemid>0)?getApplication(rs.getInt(5),true):new Application(this,itemid,rs.getString(6));
						item.addProperty("app", app.getLinkJSON());
					}
					if((otid == null) || (otid.getObjectType() != ObjectType.ENVIRONMENT)) {
						item.addProperty("env", new Environment(this, rs.getInt(7), rs.getString(8)).getLinkJSON());
					}
					item.addProperty("eventid", rs.getString(12));
				} else if(mykind.equals("hn") || mykind.equals("no")) {
					int myobjkind = rs.getInt(4);
					int objid = rs.getInt(5);
					//item.addProperty("objkind", myobjkind);
					item.addProperty("text", rs.getString(12));
					String myicon = rs.getString(13);
					if(myicon != null) {
						item.addProperty("icon", myicon);
					}
					ObjectType ot = ObjectType.fromInt(myobjkind);
					if((ot != null) && (!isObjectNewsFeed || (ot != otid.getObjectType()) || (objid != otid.getId()))) {
						switch(ot) {
						case RELEASE: item.addProperty("obj", new Application(this, objid, rs.getString(6)).getLinkJSON()); break;
						case APPLICATION:
						case APPVERSION:
							Application app = getApplication(objid,false);
							item.addProperty("obj", app.getLinkJSON());
							break;
						case COMPONENT:
						case COMPVERSION:
							Component comp = getComponent(objid,false);
							if (comp != null) item.addProperty("obj", comp.getLinkJSON());
							break;
						case ENVIRONMENT: item.addProperty("obj", new Environment(this, objid, rs.getString(6)).getLinkJSON()); break;
						case SERVER:      item.addProperty("obj", new Server(this, objid, rs.getString(6)).getLinkJSON()); break;
						case REPOSITORY:  item.addProperty("obj", new Repository(this, objid, rs.getString(6)).getLinkJSON()); break;
						case USER:        item.addProperty("obj", new User(this, objid, rs.getString(6), rs.getString(8)).getLinkJSON()); break;
						default:
							System.out.println("Unhandled object type " + ot);
							break;
						}
					}
				} else if(mykind.equals("ae")) {
					int appid = rs.getInt(5);
					int domid = getInteger(rs, 7, 0);
					//item.addProperty("objkind", myobjkind);
					String text = getString(rs, 12, null);
					String myicon = rs.getString(13);
					boolean approved = myicon.equalsIgnoreCase("Y");
					//text = "Application has been " + (approved ? "approved" : "rejected") + ((text != null) ? ("<br>" + text) : "");
					if(text != null) {
						item.addProperty("text", text);
					}
					if(myicon != null) {
						item.addProperty("icon", (approved ? "approved" : "rejected"));
					}
					if(!isObjectNewsFeed || (appid != otid.getId())) {
						item.addProperty("app", new Application(this, appid, rs.getString(6)).getLinkJSON());
					}
					if(domid != 0) {
						item.addProperty("dom", new Domain(this, domid, rs.getString(8)).getLinkJSON());
					}
				} else if(mykind.equals("rq")) {
					// Request for task (for an app)
					int taskid = rs.getInt(7);
					item.addProperty("obj", new Task(this,taskid,rs.getString(8)).getLinkJSON());
					String text = getString(rs, 12, null);
					if(text != null) {
						item.addProperty("text", text);
					}
					item.addProperty("icon", "request");
				} else if(mykind.equals("bu")) {
					// int myobjkind = rs.getInt(4);
					// int buildnumber = rs.getInt(5);
					int buildjobid = rs.getInt(7);
					//item.addProperty("objkind", myobjkind);
					item.addProperty("text", rs.getString(12));
					item.addProperty("buildjobid",buildjobid);
					String myicon = rs.getString(13);
					if(myicon != null) {
						item.addProperty("icon", myicon);
					}
					
				} else {
					System.err.println("Unexpected history item type: " + mykind);
				}
				item.addProperty("user", new User(this, rs.getInt(9), rs.getString(10), rs.getString(11)).getLinkJSON());
				item.addProperty("subs", (rs.getInt(15) > 0) ? "true" : "false");
				item.addProperty("cmnt", rs.getInt(16));
				item.addProperty("attach", rs.getInt(17));
				
				int x = rs.getInt(3);
				boolean tasks = (x == 2);	// TODO: Replace this with some real code!
				if(tasks) {
					item.addProperty("tasks", 1);
				}
				ret.addItem(item);
			}
			rs.close();
			query.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve history from database");			
	}
	
	public NewsFeedDataSet getPendingNewsForObject(ObjectTypeAndId otid, int from, int to)
	{
		if (m_userID==0) return new NewsFeedDataSet();	// Sometimes can get called from a background thread before login
		DynamicQueryBuilder query = new DynamicQueryBuilder(getDBConnection(), "");
		System.out.println("1) query string="+query.getQueryString());
		
		/*
		query.add("SELECT 'rq', r.\"when\", r.id, r.note, r.taskid, t.name, r.appid, a.name, "
				+ "  r.calendarid, c.eventname, r.userid, u.name, u.realname, "
				+ "  c.envid, e.name, "
			    + "  (SELECT max(z.startwhen) FROM ("
				+ "    SELECT r2.\"when\" AS startwhen FROM dm.dm_request r2 WHERE r2.id=r.id "
			    + "    UNION "
				+ "    SELECT c2.\"when\" AS startwhen FROM dm.dm_historycomment c2 WHERE c2.id = r.id AND c2.kind = " + ObjectType.REQUEST.value() + ")) AS z, "
				+ "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = r.id AND s.kind = " + ObjectType.REQUEST.value() + "), "
				+ "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = r.id AND c.kind = " + ObjectType.REQUEST.value() + "), "
				+ "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = r.id AND a.kind = " + ObjectType.REQUEST.value() + ") "
				+ "FROM dm.dm_request r "
				+ "LEFT OUTER JOIN dm.dm_task t ON t.id = r.taskid "
				+ "LEFT OUTER JOIN dm.dm_application a ON a.id = r.appid "
				+ "LEFT OUTER JOIN dm.dm_calendar c ON c.id = r.calendarid "
				+ "LEFT OUTER JOIN dm.dm_user u ON u.id = r.userid "
				+ "LEFT OUTER JOIN dm.dm_environment e ON e.id = c.envid "
				+ "WHERE r.status = 'N' AND (r.taskid IS NULL OR t.domainid IN (" + m_domainlist + "))");
			*/
		// Reworked to be compatible with Postgres and Oracle
		query.add("SELECT 'rq', r."+whencol+", r.id, r.note, r.taskid, t.name, r.appid, a.name, a.parentid, "
				+ "  r.calendarid, c.eventname, r.userid, u.name, u.realname, "
				+ "  e.id, e.name, "
			    + "  ("
				+ "		SELECT GREATEST(max(r2."+whencol+"),max(c2."+whencol+")) "
				+ "		FROM	dm.dm_request r2,	"
				+ "				dm.dm_historycomment	c2	"
				+ "		WHERE	r2.id = r.id	"
				+ "		AND		c2.id = r.id	"
				+ "		AND		c2.kind = " + ObjectType.REQUEST.value()
				+ "  ),"
				+ "  (SELECT count(*) FROM dm.dm_historysubs s WHERE s.id = r.id AND s.kind = " + ObjectType.REQUEST.value() + "), "
				+ "  (SELECT count(*) FROM dm.dm_historycomment c WHERE c.id = r.id AND c.kind = " + ObjectType.REQUEST.value() + "), "
				+ "  (SELECT count(*) FROM dm.dm_historyattachment a WHERE a.objid = r.id AND a.kind = " + ObjectType.REQUEST.value() + ") "
				+ "FROM dm.dm_request r "
				+ "LEFT OUTER JOIN dm.dm_task t ON t.id = r.taskid AND t.id IN (SELECT taskid FROM dm.dm_taskaccess ta,dm.dm_usersingroup ug WHERE ug.userid="+getUserID()+" AND ta.usrgrpid = ug.groupid) "
				+ "LEFT OUTER JOIN dm.dm_application a ON a.id = r.appid "
				+ "LEFT OUTER JOIN dm.dm_calendar c ON c.id = r.calendarid AND c.endtime > "+timeNow()+" "
				+ "LEFT OUTER JOIN dm.dm_user u ON u.id = r.userid "
				+ "LEFT OUTER JOIN dm.dm_environment e ON e.id = c.envid AND (e.ownerid = "+getUserID()+" OR e.ogrpid IN (SELECT groupid FROM dm.dm_usersingroup WHERE userid="+getUserID()+"))"
				+ "WHERE r.status = 'N' AND (r.taskid IS NULL OR t.domainid IN (" + m_domainlist + "))");
		query.add(" ORDER BY 2,16");
		
		System.out.println("2) query string="+query.getQueryString());
		
		try
		{
			System.out.println("About to execute query");
			ResultSet rs = query.executeQuery();
			NewsFeedDataSet ret = new NewsFeedDataSet();
			while(rs.next()) {
				int taskid = getInteger(rs, 5, 0);
				int envid = getInteger(rs,15,0);
				System.out.println("event="+rs.getString(11)+" taskid="+taskid+" envid="+envid);
				if (envid == 0 && taskid == 0) continue;	// Not for us!

				PropertyDataSet item = new PropertyDataSet();
				String mykind = rs.getString(1);
				item.addProperty("kind", mykind);	// 'rq'
				int when = rs.getInt(2);
				item.addProperty("when", when);
				item.addProperty("whenstr", formatDateToUserLocale(when));
				item.addProperty("id", rs.getInt(3));
				if(mykind.equals("rq")) {
					item.addProperty("text", rs.getString(4));
					
					if(taskid != 0) {
						item.addProperty("obj", new Task(this, taskid, rs.getString(6)).getLinkJSON());
						int appid = getInteger(rs, 7, 0);
						if(appid != 0) {
							Application app = new Application(this, appid, rs.getString(8));
							app.setParentId(rs.getInt(9));
							item.addProperty("app", app.getLinkJSON());
						}
						item.addProperty("icon", "request");							
					} else {
						int calid = getInteger(rs, 10, 0);
						if(calid != 0) {
							DMCalendarEvent evt = new DMCalendarEvent();
							evt.setID(calid);
							evt.setEventTitle(rs.getString(11));
							item.addProperty("obj", evt.getLinkJSON());							
							item.addProperty("icon", "calendar");							
						}
					}
				} else {
					// Other parts of a future union will go here
				}
				item.addProperty("user", new User(this, rs.getInt(12), rs.getString(13), rs.getString(14)).getLinkJSON());
				if (envid != 0) {
					item.addProperty("env", new Environment(this,envid, rs.getString(16)).getLinkJSON());
				}
				item.addProperty("subs",  (rs.getInt(18) > 0) ? "true" : "false");
				item.addProperty("cmnt", rs.getInt(19));
				item.addProperty("attach", rs.getInt(20));
				ret.addItem(item);
			}
			rs.close();
			query.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve pending news from database");			
	}

	public NewsFeedDataSet getCommentsForObject(ObjectTypeAndId otid)
	{
		if(otid == null) {
			throw new RuntimeException("otid must not be null");
		}
		
		String sql;
				
		switch(otid.getObjectType()) {
		case REQUEST:
		case HISTORY_NOTE:
		case APPROVAL:
		case DEPLOYMENT:
			sql = "SELECT c."+whencol+", c.note, c.userid, u.name, u.realname "
					   + "FROM dm.dm_historycomment c, dm.dm_user u "
					   + "WHERE c.id = ? AND c.kind = ? AND c.userid = u.id "
					   + "ORDER BY c."+whencol+ " DESC";
			break;
		case BUILDID:
			sql = "SELECT c."+whencol+", c.note, c.userid, u.name, u.realname "
					   + "FROM dm.dm_historycomment c, dm.dm_user u "
					   + "WHERE c.id = ? AND c.kind = ? AND c.subid = ? AND c.userid = u.id "
					   + "ORDER BY c."+whencol+ " DESC";
			break;
		default: throw new RuntimeException("Object type " + otid.getObjectType() + " cannot have comments");
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, otid.getId());
			stmt.setInt(2, otid.getObjectType().value());
			if (otid.getObjectType() == ObjectType.BUILDID) stmt.setInt(3, otid.getSubId());
			ResultSet rs = stmt.executeQuery();
			NewsFeedDataSet ret = new NewsFeedDataSet();
			while(rs.next()) {
				PropertyDataSet item = new PropertyDataSet();
				item.addProperty("when", rs.getInt(1));
				item.addProperty("text", rs.getString(2));
				item.addProperty("user", new User(this, rs.getInt(3), rs.getString(4), rs.getString(5)).getLinkJSON());
				ret.addItem(item);
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve comments from database");			
	}

	public List<Attachment> getAttachmentsForObject(ObjectTypeAndId otid)
	{
		if(otid == null) {
			throw new RuntimeException("otid must not be null");
		}
		
		String sql;
				
		switch(otid.getObjectType()) {
		case ENVIRONMENT:
		case APPLICATION:
		case APPVERSION:
		case RELEASE:
		case RELVERSION:
		case REQUEST:
		case HISTORY_NOTE:
		case DEPLOYMENT:
			sql = "SELECT a.id, a.filename, a."+sizecol+" "
					   + "FROM dm.dm_historyattachment a "
					   + "WHERE a.objid = ? AND a.kind = ?";
			break;
		default: throw new RuntimeException("Object type " + otid.getObjectType() + " cannot have attachments");
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, otid.getId());
			stmt.setInt(2, otid.getObjectType().value());
			ResultSet rs = stmt.executeQuery();
			List<Attachment> ret = new ArrayList<Attachment>();
			while(rs.next()) {
				Attachment attach = new Attachment(this, rs.getInt(1), rs.getString(2));
				attach.setSize(rs.getInt(3));
				ret.add(attach);
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve attachments from database");			
	}
	
	public Attachment getAttachment(int attachid)
	{
		String sql = "SELECT a.filename, a."+sizecol+" "
				   + "FROM dm.dm_historyattachment a "
				   + "WHERE a.id = ?";;
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, attachid);
			ResultSet rs = stmt.executeQuery();
			Attachment ret = null;
			if(rs.next()) {
				ret = new Attachment(this, attachid, rs.getString(1));
				ret.setSize(rs.getInt(2));
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve attachments from database");			
	}
	
	public InputStream getAttachmentStream(int attachid)
	{
		try
		{
			InputStream res = null;
			if (dbdriver.toLowerCase().contains("oracle")) {
				// ORACLE version
				PreparedStatement ps = getDBConnection().prepareStatement("SELECT fileoid FROM dm.dm_attachments WHERE attachmentid = ?");
				ps.setInt(1, attachid);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					res = rs.getBinaryStream(1); 
				}
				rs.close();
				ps.close();
				
			} else {
				// POSTGRES version
				LargeObjectManager lobj = ((org.postgresql.PGConnection)getDBConnection()).getLargeObjectAPI();
				PreparedStatement ps = getDBConnection().prepareStatement("SELECT fileoid FROM dm.dm_attachments WHERE attachmentid = ?");
				ps.setInt(1, attachid);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
				    // Open the large object for reading
				    long oid = rs.getLong(1);
				    LargeObject obj = lobj.open(oid, LargeObjectManager.READ);
				    res = obj.getInputStream();
				}
				rs.close();
				ps.close();
			}
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve attachment stream from database");	

	}
	
	public PropertyDataSet addNewsToObject(ObjectTypeAndId otid, String text, String icon)
	{
		if(otid == null) {
			throw new RuntimeException("otid must not be null");
		}

		switch(otid.getObjectType()) {
		case ENVIRONMENT:
		case APPLICATION:
		case APPVERSION:
		case SERVER:
		case REPOSITORY:
		case USER:
		case RELEASE:
		case RELVERSION:
		case COMPONENT:
		case COMPVERSION:
		case ACTION:
		case PROCEDURE:
		case FUNCTION:
		case USERGROUP:
			break;
		default: throw new RuntimeException("Object type " + otid.getObjectType() + " cannot have news items");
		}
		
		int when = (int) (System.currentTimeMillis()/1000);
		
		int id = this.getID("HistoryNote");
		if(id == 0) {
			throw new RuntimeException("Unable to allocate id for HistoryNote");
		}
		
		String sql;
		if(icon != null) {
			sql = "INSERT INTO dm.dm_historynote(id, objid, kind, "+whencol+", note, userid, icon) VALUES(?, ?, ?, ?, ?, ?, ?)";
		} else {
			sql = "INSERT INTO dm.dm_historynote(id, objid, kind, "+whencol+", note, userid) VALUES(?, ?, ?, ?, ?, ?)";
		}

		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, id);
			stmt.setInt(2, otid.getId());
			stmt.setInt(3, otid.getObjectType().value());
			stmt.setInt(4, when);
			stmt.setString(5, text);
			stmt.setInt(6, this.GetUserID());
			if(icon != null) {
				stmt.setString(7, icon);
			}
			stmt.execute();
			stmt.close();
			getDBConnection().commit();

			// Check if we are subscribed
			boolean subs = false;
			PreparedStatement stmt2 = getDBConnection().prepareStatement("SELECT COUNT(*) FROM dm.dm_historysubs s WHERE s.id = ? AND s.kind = ? and s.userid = ?");
			stmt2.setInt(1, otid.getId());
			stmt2.setInt(2, otid.getObjectType().value());
			stmt2.setInt(3, this.GetUserID());
			ResultSet rs = stmt2.executeQuery();
			if(rs.next()) {
				subs = (rs.getInt(1) > 0);
			}
			
			PropertyDataSet ret = new PropertyDataSet();
			ret.setNewObject(new ObjectTypeAndId(ObjectType.HISTORY_NOTE, id));
			ret.addProperty("kind", ObjectType.HISTORY_NOTE.getTypeString());
			ret.addProperty("when", when);
			ret.addProperty("whenstr", formatDateToUserLocale(when));
			ret.addProperty("id", id);
			ret.addProperty("objkind", otid.getObjectType().value());
			ret.addProperty("text", text);
			if(icon != null) {
				ret.addProperty("icon", icon);
			}
			// TODO: Need real name for user
			ret.addProperty("user", new User(this, this.GetUserID(), this.GetUserName()).getLinkJSON());
			ret.addProperty("subs", "" + subs);
			ret.addProperty("cmnt", 0);
			ret.addProperty("attach", 0);
			return ret;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return null;
	}
	
	
	public PropertyDataSet addCommentToObject(ObjectTypeAndId otid, String text)
	{
		String sql;
		switch(otid.getObjectType()) {
		case REQUEST:
		case HISTORY_NOTE:
		case DEPLOYMENT:
		case APPROVAL:
			sql = "INSERT INTO dm.dm_historycomment(id, kind, note, userid, "+whencol+") VALUES(?, ?, ?, ?, ?)";
			break;
		case BUILDID:
			sql = "INSERT INTO dm.dm_historycomment(id, kind, note, userid, "+whencol+", subid) VALUES(?, ?, ?, ?, ?, ?)";
			break;
		default: throw new RuntimeException("Object type " + otid.getObjectType() + " cannot have comments");
		}
		
		int when = (int) (System.currentTimeMillis()/1000);
		
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, otid.getId());
			stmt.setInt(2, otid.getObjectType().value());
			stmt.setString(3, text);
			stmt.setInt(4, this.GetUserID());
			stmt.setInt(5, when);
			if (otid.getObjectType() == ObjectType.BUILDID) {
				stmt.setInt(6,otid.getSubId());
			}
			stmt.execute();
			stmt.close();
			getDBConnection().commit();
			
			PropertyDataSet ret = new PropertyDataSet();
			ret.addProperty("when", when);
			ret.addProperty("text", text);
			// TODO: Need real name for user
			ret.addProperty("user", new User(this, this.GetUserID(), this.GetUserName()).getLinkJSON());
			return ret;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return null;
	}
	
	/**
	 * Associates an attachment with an object.
	 * 
	 * @param otid		- type and id of the object the attachment is associated with
	 * @param filename	- leaf filename which will be displayed
	 * @param size		- file size for display purposes
	 * @return		The id of the new attachment
	 */
	public int addAttachmentToObject(ObjectTypeAndId otid, String filename, int size, InputStream is)
	{
		int id = getID("HistoryAttachment");
		if(id == 0) {
			return 0;
		}
		try {
			System.out.println("dbdriver (a)="+dbdriver);
			if (dbdriver.toLowerCase().contains("oracle")) {
				// ORACLE version
				PreparedStatement ps = getDBConnection().prepareStatement("INSERT INTO dm.dm_attachments (attachmentid, fileoid) VALUES (?, ?)");
				ps.setInt(1, id);
				ps.setBinaryStream(2, is, size); 
				ps.executeUpdate();
				ps.close();
				is.close();
			} else {
				// POSTGRES version
				LargeObjectManager lobj = ((org.postgresql.PGConnection)getDBConnection()).getLargeObjectAPI();
				long oid = lobj.createLO(LargeObjectManager.READ | LargeObjectManager.WRITE);
				LargeObject obj = lobj.open(oid, LargeObjectManager.WRITE);
				// Copy the data from the InputStream to the large object
				byte buf[] = new byte[2048];
				int s;
				// int tl = 0;
				while ((s = is.read(buf, 0, 2048)) > 0) {
				    obj.write(buf, 0, s);
				    // tl += s;
				}
				PreparedStatement ps = getDBConnection().prepareStatement("INSERT INTO dm.dm_attachments (attachmentid, fileoid) VALUES (?, ?)");
				ps.setInt(1, id);
				ps.setLong(2, oid);
				ps.executeUpdate();
				ps.close();
				is.close();
				// Close the large object
				obj.close();
			}
			String sqlstr;
			if (dbdriver.toLowerCase().contains("oracle")) {
				sqlstr = "INSERT INTO dm.dm_historyattachment (id, objid, kind, filename, \"SIZE\") VALUES (?,?,?,?,?)";
			} else {
				sqlstr = "INSERT INTO dm.dm_historyattachment (id, objid, kind, filename, size) VALUES (?,?,?,?,?)";
			}
			PreparedStatement stmt = getDBConnection().prepareStatement(sqlstr);
			stmt.setInt(1, id);
			stmt.setInt(2, otid.getId());
			stmt.setInt(3, otid.getObjectType().value());
			stmt.setString(4, filename);
			stmt.setInt(5, size);
			stmt.execute();
			stmt.close();
			getDBConnection().commit();
			return id;
			
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return 0;
	}
	
	public void newsSubscribeObject(ObjectTypeAndId otid)
	{
		// TODO: Be prepared that the entry may already be present
		String sql = "INSERT INTO dm.dm_historysubs (id, kind, userid) VALUES (?,?,?)"; 
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, otid.getId());
			stmt.setInt(2, otid.getObjectType().value());
			stmt.setInt(3, this.GetUserID());
			stmt.execute();
			stmt.close();
			getDBConnection().commit();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}		
	}
	
	public void newsUnsubscribeObject(ObjectTypeAndId otid)
	{
		String sql = "DELETE FROM dm.dm_historysubs WHERE id = ? AND kind = ? AND userid = ?"; 
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, otid.getId());
			stmt.setInt(2, otid.getObjectType().value());
			stmt.setInt(3, this.GetUserID());
			stmt.execute();
			stmt.close();
			getDBConnection().commit();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
	}
	
	
	// Action
	
	public Action getAction(int actionid, boolean detailed)
	{
		return getAction(actionid,detailed,ObjectType.ACTION);
	}
	
	public Action getAction(int actionid, boolean detailed, ObjectType objtype)
	{
		System.out.println("getAction("+actionid+","+detailed+" ObjectType="+objtype);
		if (actionid < 0)
		{
			Action ret = new Action(this, actionid, "");
			ret.setName("");
			ret.setSummary("");  
			ActionKind actkind = ActionKind.UNCONFIGURED;
			   
			if (objtype == ObjectType.ACTION)
			    actkind = ActionKind.GRAPHICAL;
			
			ret.setKind(actkind);
			ret.setFunction(false);
			ret.setCategory(new Category(10, "General"));
			ret.setFragmentName("");
			ret.setRepository(new Repository(this, 0, ""));
			ret.setFilepath("");
			ret.setResultIsExpr(false);
			ret.setCopyToRemote(false);
			ret.setUseTTY(false);
			return ret;
		}
	 
		String sql = null;
		if(detailed) {
		 if (objtype == ObjectType.ACTION)
		 {
			sql = "SELECT a.name, a.summary, a.domainid, a.kind, a.status, a.function, a.parentid, a.usetty, "
				+ "  uc.id, uc.name, uc.realname, a.created, "
				+ "  um.id, um.name, um.realname, a.modified, "
				+ "  uo.id, uo.name, uo.realname, g.id, g.name, "
				+ "  c.id, c.name, f.name, a.repositoryid, r.name, "
				+ "  a.filepath, a.resultisexpr, a.copy, a.interpreter "
				+ "FROM dm.dm_action a "
				+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "			// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "			// modifier
				+ "LEFT OUTER JOIN dm.dm_user uo ON a.ownerid = uo.id "				// owner user
				+ "LEFT OUTER JOIN dm.dm_usergroup g ON a.ogrpid = g.id "			// owner group
				+ "LEFT OUTER JOIN dm.dm_fragments f ON a.id in (f.actionid,f.functionid) "			// fragment
				+ "LEFT OUTER JOIN dm.dm_action_categories fc on a.id = fc.id "
				+ "LEFT OUTER JOIN dm.dm_category c ON c.id = fc.categoryid "		// category
				+ "LEFT OUTER JOIN dm.dm_repository r ON r.id = a.repositoryid "	// repository
				+ "WHERE a.id = ?";
		 }
		 else
		 {
			sql = "SELECT a.name, a.summary, a.domainid, a.kind, a.status, a.function, a.parentid, a.usetty, "
			     + "  uc.id, uc.name, uc.realname, a.created, "
			     + "  um.id, um.name, um.realname, a.modified, "
			     + "  uo.id, uo.name, uo.realname, g.id, g.name, "
			     + "  c.id, c.name, f.name, a.repositoryid, r.name, "
			     + "  a.filepath, a.resultisexpr, a.copy, a.interpreter "
			     + "FROM dm.dm_action a "
			     + "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "   // creator
			     + "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "   // modifier
			     + "LEFT OUTER JOIN dm.dm_user uo ON a.ownerid = uo.id "    // owner user
			     + "LEFT OUTER JOIN dm.dm_usergroup g ON a.ogrpid = g.id "   // owner group
			     // + "LEFT OUTER JOIN dm.dm_fragments f ON f.actionid = a.id "   // fragment
			     + "LEFT OUTER JOIN dm.dm_fragments f ON a.id in (f.actionid,f.functionid) "   // fragment
			     + "LEFT OUTER JOIN dm.dm_fragment_categories fc on f.id = fc.id "	     	     
			     + "LEFT OUTER JOIN dm.dm_category c ON c.id = fc.categoryid "  // category
			     + "LEFT OUTER JOIN dm.dm_repository r ON r.id = a.repositoryid " // repository
			     + "WHERE a.id = ?";
		 }
		} else {
			sql = "SELECT a.name, a.summary, a.domainid, a.kind, a.status, a.function, a.parentid, a.usetty "
				+ "FROM dm.dm_action a WHERE a.id = ?";
		}

		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, actionid);
			ResultSet rs = stmt.executeQuery();
			Action ret = null;
			if(rs.next()) {
				ret = new Action(this, actionid, rs.getString(1));
				ret.setSummary(getString(rs, 2, ""));
				ret.setDomainId(getInteger(rs, 3, 0));				
				ActionKind actkind = ActionKind.fromInt(getInteger(rs, 4, 0));
				ret.setKind(actkind);
				getStatus(rs, 5, ret);
				ret.setFunction(getBoolean(rs, 6, false));
				ret.setParentId(getInteger(rs, 7, 0));		// for versioned/archived actions
				ret.setUseTTY(getBoolean(rs,8, false));
				
				if(detailed) {
					getCreatorModifierOwner(rs, 9, ret);
					int catid = getInteger(rs, 22, 0);
					System.out.println("catid="+catid);
					if(catid != 0) {
						System.out.println("new category, name="+rs.getString(23));
						ret.setCategory(new Category(catid, rs.getString(23)));
					}
					String fragname = rs.getString(24);
					if(fragname != null) {
						ret.setFragmentName(fragname);
					}
					int repoid = getInteger(rs, 25, 0);
					if(repoid != 0) {
						ret.setRepository(new Repository(this, repoid, rs.getString(26)));
					}
					ret.setFilepath(rs.getString(27));
					ret.setResultIsExpr(getBoolean(rs, 28, false));
					ret.setCopyToRemote(getBoolean(rs, 29, false));
					ret.setInterpreter(rs.getString(30));					
				}
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action " + actionid + " from database");			
	}
	
	private void AddActionText(int textid)
	{
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement("INSERT INTO dm.dm_actiontext(id) VALUES(?)");
			stmt.setInt(1, textid);
			stmt.execute();
			stmt.close();
		} catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
	}
	
	private void DeleteActionText(int actionid)
	{
		String sql="DELETE FROM dm.dm_actiontext WHERE id = (SELECT textid FROM dm.dm_action WHERE id=?)";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, actionid);
			stmt.execute();
			stmt.close();
		} catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
	}
	
	private void insertUpdateRecord(DMObject obj,String message,int linkid)
	{
		// Inserts a record into dm_historynote. Note, the "linkid" is used
		// by the timeline code - it is used when entries are made in "linked"
		// items. For example, when a server is added to an environment a record
		// is made in both the server's and the environment's timeline. In this
		// case, the "Server" historynote has a linkid pointing to the environment.
		// This prevents the environment timeline from displaying "linked" history
		// items whilst still displaying modifications for servers in the environment.
		//
		System.out.println("insertUpdateRecord, obj.getObjectType="+obj.getObjectType());
		String icon = null;
		switch(obj.getObjectType()) {
		case ACTION:
			icon="actionedit";
			break;
		case FUNCTION:
			icon="funcedit";
			break;
		case PROCEDURE:
			icon="procedit";
			break;
		case COMPONENT:
		case COMPVERSION:
			icon="compedit";
			break;
		case APPLICATION:
		case APPVERSION:
		case RELEASE:
			icon="appedit";
			break;
		case SERVER:
			icon="servedit";
			break;
		case ENVIRONMENT:
			icon="envedit";
			break;
		case USER:
			icon="useredit";
			break;
		case USERGROUP:
			icon="usergroupedit";
		default:
			break;
		}
		String updsql="INSERT INTO dm.dm_historynote(objid,kind,\"WHEN\",note,userid,icon,id,linkid) VAlUES(?,?,?,?,?,?,?,?)";
		try {
			long t = timeNow();
			int hnid = getID("HistoryNote");
			System.out.println("got hnid="+hnid);
			PreparedStatement updstmt = getDBConnection().prepareStatement(updsql);
			updstmt.setInt(1,obj.getId());
			updstmt.setInt(2,obj.getObjectType().value());
			updstmt.setLong(3,t);
			updstmt.setString(4,message);
			updstmt.setInt(5,getUserID());
			if (icon != null) {
				updstmt.setString(6,icon);
			} else {
				updstmt.setNull(6,Type.CHAR);
			}
			updstmt.setInt(7,hnid);
			if (linkid > 0) {
				updstmt.setInt(8,linkid);
			} else {
				updstmt.setNull(8, Type.INT);
			}
			System.out.println("inserting id "+hnid);
			updstmt.execute();
		} catch (SQLException ex) {
			ex.printStackTrace();
			rollback();
		}
	}
	
	private void RecordObjectUpdate(DMObject obj, SummaryChangeSet changes)
	{
		// Put an entry into the history table recording that the object has
		// been modified and (more pertinently) WHAT has been changed
		System.out.println("RecordObjectUpdate: timeNow()"+timeNow());
		System.out.println("obj.getCreated()="+obj.getCreated());
		if ((timeNow() - obj.getCreated()) >= 2) {
			// Creation time was more than 2 seconds ago.
			System.out.println("more than 2 secs since object creation");
			String fieldlist="";
			String sep="";
			for(SummaryField field : changes) {
				if (field.fieldname() != null) {
					if (fieldlist.length()+field.fieldname().length() < 2000) {
						fieldlist=fieldlist+sep+field.fieldname();
					}
					sep=", ";
				}
			}
			insertUpdateRecord(obj,"Modified: "+fieldlist,0);
		}
	}
	
	private void RecordObjectUpdate(DMObject obj, String desc,int linkid, boolean allowMultipleRecords)
	{
		// Put an entry into the history table recording that the object has
		// been modified. This gets a bit complicated when deleting and adding
		// nodes because multiple calls are made in succession to delete the flow
		// and then the object or add the object and then the flow.
		System.out.println("RecordObjectUpdate for "+obj.getName()+" desc:"+desc);
		String chksql="SELECT count(*) FROM dm.dm_historynote WHERE objid=? AND kind=? and userid=? and \"WHEN\" > ?";
		try {
			boolean secondInSuccession=false;
			long t = timeNow();
			PreparedStatement chkstmt = getDBConnection().prepareStatement(chksql);
			chkstmt.setInt(1,obj.getId());
			chkstmt.setInt(2,obj.getObjectType().value());
			chkstmt.setInt(3,getUserID());
			chkstmt.setLong(4,t-2);	// Within 2 seconds of the same object and the same user
			ResultSet chkrs = chkstmt.executeQuery();
			if (chkrs.next()) {
				int c = chkrs.getInt(1);
				System.out.println("c="+c);
				if (c>0) secondInSuccession=true;
			}
			chkrs.close();
			chkstmt.close();
			System.out.println("secondInSuccession="+secondInSuccession+" allowMultipleRecords="+allowMultipleRecords);
			if (!secondInSuccession || allowMultipleRecords) {
				// Not a second update in quick succession - record it
				insertUpdateRecord(obj,desc,linkid);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			rollback();
		}
	}
	
	private void RecordObjectUpdate(DMObject obj, String desc)
	{
		RecordObjectUpdate(obj,desc,0,false);
	}
	
	public void RecordObjectUpdateMultiple(DMObject obj, String desc)
	{
		RecordObjectUpdate(obj,desc,0,true);
	}
	
	private void RecordObjectUpdate(DMObject obj, String desc,int linkid)
	{
		RecordObjectUpdate(obj,desc,linkid,false);
	}
	
	public boolean updateAction(Action act, SummaryChangeSet changes)
	{
		Category cat = null;
		String fragname = null;
		String fragsumm = null;
		long t = timeNow();
		
		System.out.println("updateAction");
		
		ArchiveAction(act);
		
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_action ");
		update.add("SET modified = ?, modifierid = ?", t, getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case ACTION_KIND: {
				ActionKind kind = (ActionKind) changes.get(field);
				update.add(", kind = ?", (kind != null) ? kind.value() : ActionKind.UNCONFIGURED.value());
				if (kind == ActionKind.IN_DB) {
					int textid = getID("actiontext");
					update.add(", textid = ?", textid);
					AddActionText(textid);
				} else {
					// We need to delete the old action text if we're changing from an action
					// that was stored in the database.
					DeleteActionText(act.getId());
				}
				}
				break;
			case ACTION_CATEGORY:
				cat = (Category) changes.get(field);
				break;
			case ACTION_FRAGNAME:
				fragname = (String) changes.get(field);
				break;
			case ACTION_REPO: {
				DMObject repo = (DMObject) changes.get(field);
				update.add(", repositoryid = ?", (repo != null) ? repo.getId() : Null.INT);
				}
				break;
			case ACTION_FILEPATH:  update.add(", filepath = ?", (String) changes.get(field)); break;
			case ACTION_INTERPRETER:  update.add(", interpreter = ?", (String) changes.get(field)); break;			
			case ACTION_RESISEXPR: update.add(", resultisexpr = ?", changes.getBoolean(field)); break;
			case ACTION_COPYTOREM: update.add(", copy = ?", changes.getBoolean(field)); break;
			case ACTION_USETTY: update.add(", usetty = ?", changes.getBoolean(field)); break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					if(field == SummaryField.SUMMARY) {
						fragsumm = (String) changes.get(field);
					}
					updateObjectSummaryField(act,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", act.getId());
		
		try {
			update.execute();
			
			Category oldcat = act.getCategory();
			if (cat != null) System.out.println("cat is "+cat.getId());
			else System.out.println("cat is null");
			if (oldcat != null) System.out.println("oldcat is "+oldcat.getId());
			else System.out.println("oldcat is null");
			if (cat == null) {
				// Category was not passed (because it wasn't changed). 
				cat = oldcat;
				if (cat == null) {
					cat = new Category(10,"General");
				} else {
					if (cat.getId()==0) {
						// No category - set to general
						cat = new Category(10,"General");
					}
				}
			}
			System.out.println("after override");
			if (cat != null) System.out.println("cat is now "+cat.getId());
			
			if (((cat == null) && (oldcat != null) && (oldcat.getId() != 0))
					|| ((cat != null) && (cat.getId() != 0))) {
				// Category is set, so ensure that we have a fragment row for this action
				System.out.println("Category set - attempting update (cat is "+cat);
				DynamicQueryBuilder update2 = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_fragments ");
				update2.add("SET modifierid = ?, modified = ?", getUserID(), timeNow());
				if(fragname != null) {
					update2.add(", name = ?", fragname);
				}
				if(fragsumm != null) {
					update2.add(", summary = ?", fragsumm);
				}
				update2.add(" WHERE ? IN (actionid,functionid)", act.getId());
				update2.execute();
				int updcount = update2.getUpdateCount();
				update2.close();
				
				if (cat != null) {
				    String sql1="DELETE from dm.dm_fragment_categories a where a.id in (select b.id from dm.dm_fragments b where ? in (b.actionid,b.functionid))";  // only allow 1 category at this time
				    String sql2="INSERT INTO dm.dm_fragment_categories select b.id, ? from dm.dm_fragments b where ? in (b.actionid,b.functionid)";
				    PreparedStatement stmt = getDBConnection().prepareStatement(sql1);
				    stmt.setInt(1, act.getId());
				    stmt.execute();
				    stmt.close();
				    stmt = getDBConnection().prepareStatement(sql2);
				    stmt.setInt(1, cat.getId());
				    stmt.setInt(2, act.getId());
				    stmt.execute();
				    stmt.close();
     
				    sql1="DELETE from dm.dm_action_categories where id = ?";  // only allow 1 category at this time
				    sql2="INSERT INTO dm.dm_action_categories (id,categoryid) VALUES(?,?)";
				    stmt = getDBConnection().prepareStatement(sql1);
				    stmt.setInt(1, act.getId());
				    stmt.execute();
				    stmt.close();
				    stmt = getDBConnection().prepareStatement(sql2);
				    stmt.setInt(1, act.getId());
				    stmt.setInt(2,cat.getId());
				    stmt.execute();
				    stmt.close();
    			}
				
				if (updcount == 0) {
					// TODO: Possibly add drilldown for graphical actions
					System.out.println("Category set - attempting insert of "+(act.isFunction()?"FUNCTION":"ACTION"));
					int newid = getID("fragments");
					if(newid > 0) {
						String sql1="INSERT INTO dm.dm_fragments(id,name,summary,categoryid,exitpoints,creatorid,created,modifierid,modified,actionid)   VALUES(?,?,?,?,1,?,?,?,?,?)";
						String sql2="INSERT INTO dm.dm_fragments(id,name,summary,categoryid,exitpoints,creatorid,created,modifierid,modified,functionid) VALUES(?,?,?,?,1,?,?,?,?,?)";
						PreparedStatement stmt = getDBConnection().prepareStatement(act.isFunction()?sql2:sql1);
						stmt.setInt(1, newid);
						stmt.setString(2, (fragname != null) ? fragname : act.getName());
						stmt.setString(3, (fragsumm != null) ? fragsumm : act.getSummary());
						stmt.setInt(4, (cat != null) ? cat.getId() : oldcat.getId());
						stmt.setInt(5, getUserID());
						stmt.setLong(6, t);
						stmt.setInt(7, getUserID());
						stmt.setLong(8, t);
						stmt.setInt(9,act.getId());
						stmt.execute();
						stmt.close();
						
						// ObjectType ot=(act.getKind() == ActionKind.GRAPHICAL)?ObjectType.ACTION:ObjectType.FRAGMENT;
						// ObjectTypeAndId otid = new ObjectTypeAndId(ot,newid);
						// addToCategory(cat != null ? cat.getId() : oldcat.getId(),otid);
						if (act.getKind() == ActionKind.GRAPHICAL)
	      				sql1="INSERT INTO dm.dm_action_categories values (?,?)";
						else
						sql1="INSERT INTO dm.dm_fragment_categories values (?,?)";
      					stmt = getDBConnection().prepareStatement(sql1);
      					stmt.setInt(1, newid);
      					stmt.setInt(2, (cat != null) ? cat.getId() : oldcat.getId());
      					stmt.execute();
      					stmt.close();
      
						// Now add the fragmentattrs
						System.out.println("Inserting into fragmentattrs for actionid="+act.getId());
						PreparedStatement stmt2 = getDBConnection().prepareStatement(
								"INSERT INTO dm.dm_fragmentattrs(id,typeid,atname,attype,atorder,required) "
								+ "VALUES(?,?,?,?,?,?)");
						
						// Loop through the arguments list, adding any that are not "A" (always)
						PreparedStatement qst = getDBConnection().prepareStatement(
								"SELECT name,type,inpos,required FROM dm.dm_actionarg WHERE actionid=? AND (switchmode is null or switchmode<>'A') order by inpos, name");
						qst.setInt(1,act.getId());
						ResultSet rs = qst.executeQuery();
						int maxorder=0;
						String plist="";
						while (rs.next()) {
							int fragid = getID("fragmentattrs");
							stmt2.setInt(1,fragid);
							stmt2.setInt(2,newid);
							String pname = rs.getString(1);
							if (plist.length()>0) plist=plist+",";
							plist=plist+"@"+pname.toLowerCase()+"@";
							stmt2.setString(3,pname);
							stmt2.setString(4,rs.getString(2));
							int atorder = rs.getInt(3);
							if (atorder > maxorder) maxorder = atorder;
							stmt2.setInt(5,atorder);
							stmt2.setString(6,rs.getString(4));
							stmt2.execute();
						}
						rs.close();
						qst.close();
						if (act.isFunction()) {
							System.out.println("act is a function, inserting result field");
							// Add the "result" into fragmentattrs
							PreparedStatement rst = getDBConnection().prepareStatement(
									"INSERT INTO dm.dm_fragmentattrs(id,typeid,atname,attype,atorder,inherit,required) "
									+ "VALUES(?,?,'result','entry',?,'R','Y')");
							int fragid = getID("fragmentattrs");
							rst.setInt(1,fragid);
							rst.setInt(2,newid);
							rst.setInt(3,maxorder+1);
							rst.execute();
							// And add the function text into fragmentattrs
							String ft = "set @result@ = "+act.getName()+"("+plist+");";
							PreparedStatement fst = getDBConnection().prepareStatement(
									"INSERT INTO dm.dm_fragmenttext(fragmentid,data,type) values(?,?,0)");
							fst.setInt(1,newid);
							fst.setString(2,ft);
							fst.execute();
							fst.close();
						}
					}
				}
			} else {
				// Category is not set, so remove any fragment row for this action
				System.out.println("Category not set - attempting delete");
				String dsql[] = new String[4];
				dsql[0] = "DELETE FROM dm.dm_fragmenttext WHERE fragmentid IN (SELECT id FROM dm.dm_fragments WHERE ? in (functionid,actionid))";
				dsql[1] = "DELETE FROM dm.dm_fragmentattrs WHERE typeid IN (SELECT id FROM dm.dm_fragments WHERE ? in (functionid,actionid))";
				dsql[2] = "DELETE FROM dm.dm_fragments WHERE ? in (functionid,actionid)";
				dsql[3] = "DELETE FROM dm.dm_fragment_categories WHERE id IN (SELECT id FROM dm.dm_fragments WHERE ? in (functionid,actionid))";
				
				for (int i=0;i<dsql.length;i++) {
					PreparedStatement stmt = getDBConnection().prepareStatement(dsql[i]);
					System.out.println("executing "+dsql[i]);
					stmt.setInt(1,act.getId());
					stmt.execute();
					stmt.close();
				}
			}	
			RecordObjectUpdate(act,changes);
			System.out.println("Committing");
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}

		System.out.println("Something somewhere went badly wrong");
		return false;
	}
 
 public void updateOrder(Action action, ArrayList<String> updates)
 {
  ArchiveAction(action);
  updateModTime(action);
  RecordObjectUpdate(action,"Argument Order Changed");
  try
  {
   PreparedStatement stmt = getDBConnection().prepareStatement("UPDATE dm.dm_actionarg set outpos = null where actionid = " + action.getId());
   stmt.execute();
   stmt.close();
   
   for (int i=0;i<updates.size();i++)
   {
    String key = updates.get(i); 
    String usql = "UPDATE dm.dm_actionarg set outpos = " + (i+1) + " where actionid = " + action.getId() + " and name = '" + key + "'";
    System.out.println(usql);
    PreparedStatement stmt2 = getDBConnection().prepareStatement(usql);
    stmt2.execute();
    stmt2.close();
   }
   
   getDBConnection().commit();
  }
  catch (SQLException e)
  {
   e.printStackTrace();
   rollback();
  }
 }

 public void updateInputOrder(Action action, HashMap<String, String> updates)
 {
	 ArchiveAction(action);
	 updateModTime(action);
	 RecordObjectUpdate(action,"Argument Order Changed 2");
	 Category cat = action.getCategory();
	 boolean hasCategory = (cat != null) && (cat.getId() != 0);
	 try {
		 for (Map.Entry<String, String> entry : updates.entrySet()) {
			 String key = entry.getKey();
			 String value = (String) entry.getValue();
			 String usql1 = "UPDATE dm.dm_actionarg set inpos = " + value + " where actionid = " + action.getId() + " and name = '" + key + "'";
			 String usql2 = "UPDATE dm.dm_fragmentattrs set atorder = " + value + "where atname = '" + key + "' and typeid =  "
					 	+ 	" (SELECT id FROM dm.dm_fragments x WHERE " + action.getId() + " in (x.actionid,x.functionid))";
			 PreparedStatement stmt1 = getDBConnection().prepareStatement(usql1);
			 stmt1.execute();
			 stmt1.close();
			 if (hasCategory) {
				 PreparedStatement stmt2 = getDBConnection().prepareStatement(usql2);
				 stmt2.execute();
				 stmt2.close();
			 }
		 }
		 if (action.isFunction()) {
			 
			 PreparedStatement ts = getDBConnection().prepareStatement("SELECT id FROM dm.dm_fragments WHERE ? IN (actionid,functionid)");
			 ts.setInt(1, action.getId());
			 ResultSet rs = ts.executeQuery();
			 if (rs.next()) {
				 int typeid=rs.getInt(1);
				 UpdateFunctionFragmentText(action,typeid);
			 }
			 rs.close();
			 ts.close();
		 }
		 getDBConnection().commit();
	 } catch (SQLException e) {
		 e.printStackTrace();
		 rollback();
	 }
 }
 
 
 public void removeArg(int id, String name)
 {

   String usql = "UPDATE dm.dm_actionarg set outpos = null where actionid = " + id + " and name = '" + name + "'";

   try
   {
    PreparedStatement stmt = getDBConnection().prepareStatement(usql);
    stmt.execute();

    getDBConnection().commit();
    stmt.close();
   }
   catch (SQLException e)
   {
    e.printStackTrace();
    rollback();
   }
 }

 public void updateSwitch(int id, String name, String flag)
 {

   String usql = "UPDATE dm.dm_actionarg set switch = ? where actionid = " + id + " and name = '" + name + "'";

   try
   {
    PreparedStatement stmt = getDBConnection().prepareStatement(usql);
    stmt.setString(1,flag);
    stmt.execute();

    getDBConnection().commit();
    stmt.close();
   }
   catch (SQLException e)
   {
    e.printStackTrace();
    rollback();
   }
 }
	
 public void addSwitch(int id, String name, String flag)
 {

   String usql = "insert into dm.dm_actionarg values (?,?,'false',null,'N',?,'N',null,'A',null)";

   try
   {
    PreparedStatement stmt = getDBConnection().prepareStatement(usql);
    stmt.setInt(1,id);
    stmt.setString(2, name);
    if (flag.trim().length() == 0)
     stmt.setNull(3,java.sql.Types.CHAR );
    else
     stmt.setString(3, flag);
    stmt.execute();

    getDBConnection().commit();
    stmt.close();
   }
   catch (SQLException e)
   {
    e.printStackTrace();
    rollback();
   }
 }
 
	public String SaveProcBody(int actionid,String procbody)
	{
		System.out.println("SaveProcBody actionid="+actionid);
		Action action = getAction(actionid,true);
		
		Domain dom = action.getDomain();
		Engine eng = (dom != null) ? dom.findNearestEngine() : null;
		if(eng == null) {
			System.err.println("ERROR: Could not find engine to encrypt data");
			return "ERROR: Could not find engine to Parse Body";
		}
		String parseResult = eng.ParseProcedure("action "+action.getName()+"{"+procbody+"}");
		System.out.println("parseResult = "+parseResult);
		if (parseResult.contains("parsed ok")) {
			parseResult="";
			String usql = "UPDATE dm.dm_actiontext SET data=? WHERE id=(SELECT b.textid FROM dm.dm_action b where b.id=?)";
			try {
				PreparedStatement stmt = getDBConnection().prepareStatement(usql);
				stmt.setString(1,procbody);
				stmt.setInt(2, actionid);
				stmt.execute();
				int uc = stmt.getUpdateCount();
				System.out.println("UpdateCount = "+uc);
				updateModTime(action);
				RecordObjectUpdate(action,"DMScript Body Changed");
				getDBConnection().commit();
				stmt.close();
			} catch(SQLException e) {
				e.printStackTrace();
				rollback();
			}
		}
		return parseResult;
	}
	
	
	public boolean updateActionArgs(Action act, ACDChangeSet<Action.ActionArg> changes)
	{
		ArchiveAction(act);
		updateModTime(act);
		RecordObjectUpdate(act,"Input Arguments Changed");
		System.out.println("updateActionArgs");
		String dsql = "DELETE FROM dm.dm_actionarg aa WHERE aa.actionid = ? AND aa.name = ?";
		String asql = "INSERT INTO dm.dm_actionarg(actionid,name,inpos,outpos,required,pad,switchmode,switch,negswitch,type) VALUES(?,?,?,?,?,?,?,?,?,?)";
		String csql = "UPDATE dm.dm_actionarg aa SET name = ?, inpos = ?, outpos = ?,"
			+ " required = ?, pad = ?, switchmode = ?, switch = ?, negswitch = ?, type=? WHERE aa.actionid = ? AND aa.name = ?";
		String fqsql = "SELECT f.id FROM dm.dm_fragments f WHERE ? in (f.actionid,f.functionid)";
		String fdsql = "DELETE FROM dm.dm_fragmentattrs fa WHERE fa.typeid = ? AND fa.atname = ?";
		String fasql = "INSERT INTO dm.dm_fragmentattrs(id,typeid,attype,atname,atorder,required) VALUES(?,?,?,?,?,?)";
		String fcsql = "UPDATE dm.dm_fragmentattrs fa SET atname = ?, attype = ?, required = ? WHERE fa.typeid = ? AND fa.atname = ?";
		try {
			Category cat = act.getCategory();
			boolean hasCategory = (cat != null) && (cat.getId() != 0);
			int fragmentId = 0;
			System.out.println("hasCategory="+hasCategory);
			
			PreparedStatement stmt = null;
			if(hasCategory) {
				stmt = getDBConnection().prepareStatement(fqsql);
				stmt.setInt(1, act.getId());
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					fragmentId = rs.getInt(1);
				}
				if(fragmentId == 0) {
					System.out.println("ERROR: Failed to find fragment for action " + act.getId() + " '" + act.getName() +"'");
					hasCategory = false;
				}
			}
			
			stmt = getDBConnection().prepareStatement(dsql);
			for(Action.ActionArg a : changes.deleted()) {
				System.out.println("Deleting " + act.getId() + " '" + a.getId() + "' from actionarg");
				stmt.setInt(1, act.getId());
				stmt.setString(2, a.getId());		// This is the old name
				stmt.execute();
			}
			stmt.close();
			
			if(hasCategory) {
				stmt = getDBConnection().prepareStatement(fdsql);
				for(Action.ActionArg a : changes.deleted()) {
					System.out.println("Deleting " + fragmentId + " '" + a.getId() + "' from fragmentattrs");
					stmt.setInt(1, fragmentId);
					stmt.setString(2, a.getId());		// This is the old name
					stmt.execute();
				}
				stmt.close();
				
			}
			
			PreparedStatement maxinpos = getDBConnection().prepareStatement("select max(inpos) from dm.dm_actionarg where actionid = " + act.getId());
   ResultSet rs1 = maxinpos.executeQuery();
   int inpos = 1;
   if (rs1.next()) 
   {
    inpos = rs1.getInt(1) + 1;
   }
   rs1.close();
   maxinpos.close();
   
			stmt = getDBConnection().prepareStatement(asql);
			for(Action.ActionArg a : changes.added()) {
				System.out.println("Inserting " + act.getId() + " '" + a.getName() + "' into actionarg");	
				stmt.setInt(1, act.getId());
				stmt.setString(2, a.getName());		// This is the new name
				setIntegerIfGreaterThanZero(stmt, 3,inpos);
				setIntegerIfGreaterThanZero(stmt, 4, a.getOutputPosition());
				stmt.setString(5, a.isRequired() ? "Y" : "N");
				stmt.setString(6, a.isPad() ? "Y" : "N");
				stmt.setString(7, a.getSwitchMode().value());
				if (a.getSwitch().trim().length() == 0)
				  stmt.setNull(8, java.sql.Types.CHAR);
				else
				 stmt.setString(8, a.getSwitch());
				
				if (a.getNegSwitch().trim().length() == 0)
					stmt.setNull(9, java.sql.Types.CHAR);
				else				
					stmt.setString(9, a.getNegSwitch());
    
				stmt.setString(10, a.getType());
				stmt.execute();
			}
			stmt.close();
			
			if(hasCategory) {
				// String fasql = "INSERT INTO dm.dm_fragmentattrs(id,typeid,attype,atname,atorder,required) VALUES(?,?,?,?,?,?)";
				stmt = getDBConnection().prepareStatement(fasql);
				for(Action.ActionArg a : changes.added()) {
					SwitchMode sm = a.getSwitchMode();
					if (!(sm != null && sm.value() != null && sm.value().equalsIgnoreCase("A"))) {
						// Not an "always" flag type
						System.out.println("Inserting " + fragmentId + " '" + a.getName() + "' into fragmentattrs");
						int newid = getID("fragmentattrs");
						stmt.setInt(1, newid);
						stmt.setInt(2, fragmentId);
						stmt.setString(3, a.getType());
						stmt.setString(4, a.getName());
						stmt.setInt(5,a.getInputPosition());
						stmt.setString(6, a.isRequired()?"Y":"N");
						System.out.println("name="+a.getName()+" inpos="+a.getInputPosition()+" outpos="+a.getOutputPosition());
						stmt.execute();
					}
				}
				stmt.close();
			}

			stmt = getDBConnection().prepareStatement(csql);
			for(Action.ActionArg a : changes.changed()) {
				System.out.println("Updating " + act.getId() + " '" + a.getId() + "' in actionarg");
				stmt.setString(1, a.getName());
				setIntegerIfGreaterThanZero(stmt, 2, a.getInputPosition());
				setIntegerIfGreaterThanZero(stmt, 3, a.getOutputPosition());
				stmt.setString(4, a.isRequired() ? "Y" : "N");
				stmt.setString(5, a.isPad() ? "Y" : "N");
				stmt.setString(6, a.getSwitchMode().value());
				String sm = a.getSwitch();
				if (sm.trim().length()>0) {
					stmt.setString(7,sm);
				} else {
					stmt.setNull(7,Type.CHAR);
				}
				String nsm = a.getNegSwitch();
				if (nsm.trim().length()>0) {
					stmt.setString(8,nsm);
				} else {
					stmt.setNull(8,Type.CHAR);
				}
				stmt.setString(9, a.getType());
				stmt.setInt(10, act.getId());
				stmt.setString(11, a.getId());		// This is the old name		
				stmt.execute();
			}
			stmt.close();
			
			if(hasCategory) {
				// String fcsql = "UPDATE dm.dm_fragmentattrs fa SET atname = ?, attype = ?, required = ? WHERE fa.typeid = ? AND fa.atname = ?";
				stmt = getDBConnection().prepareStatement(fcsql);
				for(Action.ActionArg a : changes.changed()) {
					PreparedStatement s2 = getDBConnection().prepareStatement("SELECT attype FROM dm.dm_fragmentattrs WHERE typeid=? AND atname=?");
					s2.setInt(1,fragmentId);
					s2.setString(2, a.getId());
					ResultSet rs2 = s2.executeQuery();
					rs2.next();
					String origattype = rs2.getString(1);
					rs2.close();
					s2.close();
					System.out.println("Changes to argument '"+a.getId()+"'");
					System.out.println("Switchmode="+a.getSwitchMode().value());
					SwitchMode sm = a.getSwitchMode();
					if (sm != null && sm.value() != null && sm.value().equalsIgnoreCase("A")) {
						System.out.println("Deleting from fragmentattrs where typeid="+fragmentId+" and atname="+a.getId());
						//
						// Delete any fragmentattrs relating to switches which are now "always" type flags
						//
						String dafsql="DELETE FROM dm.dm_fragmentattrs WHERE typeid=? AND atname=?";
						PreparedStatement stmt1 = getDBConnection().prepareStatement(dafsql);
						stmt1.setInt(1, fragmentId);
						stmt1.setString(2, a.getId());		// This is the old name
						stmt1.execute();
						System.out.println("DELETE FROM dm.dm_fragmentattrs WHERE typeid="+fragmentId+" AND atname='"+a.getId()+"'");
						System.out.println("update count = "+stmt1.getUpdateCount());
						stmt1.close();
					} else {
						System.out.println("Updating " + fragmentId + " '" + a.getId() + "' in fragmentattrs");
						stmt.setString(1, a.getName());				// This is the new name
						if (origattype.equalsIgnoreCase("dropdown")) {
							// If the fragment attribute has been set to dropdown then don't change it
							stmt.setString(2, "dropdown");
						} else {
							stmt.setString(2, a.getType());				// This is the new type
						}
						stmt.setString(3, a.isRequired()?"Y":"N");	// Required flag
						stmt.setInt(4, fragmentId);
						stmt.setString(5, a.getId());				// This is the old name
						stmt.execute();
						if(stmt.getUpdateCount() == 0) {
							// Didn't find a row to update - insert it
							System.out.println("Inserting missing " + fragmentId + " '" + a.getId() + "' in fragmentattrs");
							// String fasql = "INSERT INTO dm.dm_fragmentattrs(id,typeid,attype,atname,atorder,required) VALUES(?,?,?,?,1,?)";
							PreparedStatement stmt2 = getDBConnection().prepareStatement(fasql);
							int newid = getID("fragmentattrs");
							stmt2.setInt(1, newid);
							stmt2.setInt(2, fragmentId);
							stmt2.setString(3, a.getType());
							stmt2.setString(4, a.getName());		// This is the new name
							stmt2.setString(5, a.isRequired()?"Y":"N");
							stmt2.execute();
						}
					}
				}
				stmt.close();
				if (act.isFunction()) {
					UpdateFunctionFragmentText(act,fragmentId);
				}
			}

			getDBConnection().commit();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	private void UpdateFunctionFragmentText(Action act,int fragmentid)
	{
		try {
			// result field has inherit set to "R"
			PreparedStatement stmt = getDBConnection().prepareStatement("SELECT atname,atorder FROM dm.dm_fragmentattrs WHERE inherit IS NULL AND typeid=? ORDER BY atorder");
			stmt.setInt(1,fragmentid);
			ResultSet rs = stmt.executeQuery();
			String plist="";
			String sep="";
			int maxorder=0;
			while (rs.next()) {
				plist = plist + sep + "@" + rs.getString(1).toLowerCase() + "@";
				sep=",";
				int atorder = rs.getInt(2);
				if (atorder > maxorder) maxorder = atorder;
			}
			rs.close();
			stmt.close();
			int resorder = maxorder+1;
			String ft = "set @result@ = "+act.getName()+"("+plist+");";
			PreparedStatement fst = getDBConnection().prepareStatement("UPDATE dm.dm_fragmenttext SET data=? WHERE fragmentid=?");
			fst.setString(1,ft);
			fst.setInt(2,fragmentid);
			fst.execute();
			fst.close();
			// Now update result position
			PreparedStatement rst = getDBConnection().prepareStatement("UPDATE dm.dm_fragmentattrs SET atorder=? WHERE typeid=? AND inherit='R'");
			rst.setInt(1,resorder);
			rst.setInt(2,fragmentid);
			rst.execute();
			rst.close();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
			
			
	}

	
	public List<ActionArg> getActionArgsForInput(Action action)
	{
		String sql = "SELECT aa.name, aa.required, aa.pad, aa.inpos, aa.outpos,"
			+ " aa.switchmode, aa.switch, aa.negswitch, aa.type FROM dm.dm_actionarg aa "
			+ "WHERE aa.actionid = ? ORDER BY aa.inpos, aa.name";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, action.getId());
			ResultSet rs = stmt.executeQuery();
			List<ActionArg> ret = new ArrayList<ActionArg>();
			while(rs.next()) {
				String name = rs.getString(1);
				String type = rs.getString(9);
				ret.add(action.new ActionArg(name, name, type,
					getBoolean(rs, 2, false), getBoolean(rs, 3, false),
					getInteger(rs, 4, 0), getInteger(rs, 5, 0),
					Action.SwitchMode.fromString(rs.getString(6)),
					rs.getString(7), rs.getString(8)));
				System.out.println(name +"=" +  getInteger(rs, 5, 0));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action args for action " + action.getId() + " from database");				
	}

	
	public List<ActionArg> getActionArgsForPalette(Action action)
	{
		String sql = "SELECT aa.name, aa.required FROM dm.dm_actionarg aa "
			+ "WHERE aa.actionid = ? AND aa.outpos IS NULL AND (aa.switchmode IS NULL OR aa.switchmode <> 'A') ORDER BY aa.inpos, aa.name";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, action.getId());
			ResultSet rs = stmt.executeQuery();
			List<ActionArg> ret = new ArrayList<ActionArg>();
			while(rs.next()) {
				String name = rs.getString(1);
				String type = "entry";
				ret.add(action.new ActionArg(name, name, type, getBoolean(rs, 2, false)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action args for action " + action.getId() + " from database");				
	}

	
	public List<ActionArg> getActionArgsForOutput(Action action)
	{
		String sql = "SELECT aa.name, aa.required, aa.pad, aa.inpos, aa.outpos,"
			+ " aa.switchmode, aa.switch, aa.negswitch FROM dm.dm_actionarg aa "
			+ "WHERE aa.actionid = ? AND aa.outpos IS NOT NULL ORDER BY aa.outpos";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, action.getId());
			ResultSet rs = stmt.executeQuery();
			List<ActionArg> ret = new ArrayList<ActionArg>();
			while(rs.next()) {
				String name = rs.getString(1);
				String type = "entry";
				ret.add(action.new ActionArg(name, name, type,
					getBoolean(rs, 2, false), getBoolean(rs, 3, false),
					getInteger(rs, 4, 0), getInteger(rs, 5, 0),
					Action.SwitchMode.fromString(rs.getString(6)),
					rs.getString(7), rs.getString(8)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action args for action " + action.getId() + " from database");				
	}

	
	public String getActionText(Action action)
	{
		String sql = "SELECT t.data FROM dm.dm_action a, dm.dm_actiontext t WHERE t.id = a.textid AND a.id = ?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, action.getId());
			ResultSet rs = stmt.executeQuery();
			String ret = "";
			if(rs.next()) {
				ret = rs.getString(1);
				if (rs.wasNull()) ret="";
			}
			rs.close();
			stmt.close();
			System.out.println("getActionText returns ["+ret+"]");
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action text for action " + action.getId() + " from database");			
	}
	
	public List<Action> getAccessibleActions(String objid,int domainid)
	{
		System.out.println("getAccessibleActions("+objid+","+domainid+")");
		if (objid.startsWith("task")) {
			objid="ta"+objid.substring(4);
		}
		ObjectTypeAndId x = new ObjectTypeAndId(objid);
		Domain d = null;
		if (x.getId()>0) {
			// Get domain from object
			DMObject obj = this.getObject(x.getObjectType(), x.getId());
			System.out.println("obj = "+obj.getName());
			d =  obj.getDomain();
		} else {
			// New object - get domain from tree
			d = getDomain(domainid);
		}
		Hashtable<Integer,char[]> accessRights = new Hashtable<Integer,char[]>();
		try
		{
			System.out.println("getting tasks for domain "+d.getId());
			String sql1 = "select a.id,b.viewaccess,b.writeaccess	"
					+	"from	dm.dm_action		a,	"
					+	"		dm.dm_actionaccess	b,	"
					+	"		dm.dm_usersingroup	c	"
					+	"where	c.userid=?				"
					+	"and	c.groupid=b.usrgrpid	"
					+	"and	a.id=b.actionid			"
					+	"and	a.domainid=?			"
					+	"union	"
					+	"select	d.id,e.viewaccess,e.writeaccess	"
					+	"from	dm.dm_action		d,	"
					+	"		dm.dm_actionaccess	e	"
					+	"where	e.usrgrpid=1			"	// user group 1 = Everyone
					+	"and	d.id=e.actionid			"
					+	"and	d.domainid=?			";
			
			String sql2 = "SELECT a.id, a.name, a.kind, a.function, a.domainid "
					+ "FROM dm.dm_action a WHERE a.domainid = ? AND a.status = 'N' AND a.pluginid IS NULL ORDER BY 2";
			List<Action> ret = new ArrayList<Action>();
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			while (d != null && d.getId()>=1) {
				System.out.println("domain is "+d.getName()+") read="+d.isReadable(true)+" write="+d.isWriteable(true)+" update="+d.isUpdatable(true)+" view="+d.isViewable(true));
				if (!m_OverrideAccessControl) {
					// Get a list of actions in this domain with non-default access permissions
					System.out.println("no override access, getting list of overrides userid="+getUserID()+" domainid="+d.getId());
					stmt1.setInt(1,getUserID());
					stmt1.setInt(2,d.getId());
					stmt1.setInt(3,d.getId());
					ResultSet rs = stmt1.executeQuery();
					while (rs.next()) {
						char[] ar = new char[2];
						ar[0] = getString(rs,2,"-").charAt(0);
						ar[1] = getString(rs,3,"-").charAt(0);
						System.out.println("id="+rs.getInt(1)+" ar[0]="+ar[0]+" ar[1]="+ar[1]);
						accessRights.put(rs.getInt(1),ar);
					}
					rs.close();
				}
				stmt2.setInt(1,d.getId());
				ResultSet rs = stmt2.executeQuery();
				while(rs.next()) {
					boolean include=false;
					Action act = new Action(this, rs.getInt(1), rs.getString(2));
					if (!m_OverrideAccessControl) {
						// Need to check permissions on this action. Only include it if it's both
						// viewable and writable (execute rights)
						char[] ar = accessRights.get(act.getId());
						if (ar != null) {
							System.out.println("ar[0]="+ar[0]+" ar[1]="+ar[1]);
							boolean viewable=(ar[0]=='Y')?true:(ar[0]=='-')?d.isViewable(true):false; 
							boolean executable=(ar[1]=='Y')?true:(ar[1]=='-')?d.isWriteable(true):false; 
							System.out.println("viewable="+viewable+" executable="+executable);
							if (viewable && executable) include=true;
						} else {
							// No override record
							System.out.println("no override record for "+act.getId()+" ("+act.getName()+"), taking domain permissions d.isViewable(true)="+d.isViewable(true)+" d.isWriteable(true)="+d.isWriteable(true));
							include = (d.isViewable(true) && d.isWriteable(true));
						}
					} else {
						include=true;	// override access control
					}
					if (include) {
						ActionKind actkind = ActionKind.fromInt(getInteger(rs, 3, 0));
						String isfunc = rs.getString(4);
						if (isfunc.equalsIgnoreCase("Y")) act.setFunction(true);
						act.setDomainId(rs.getInt(5));
						act.setKind(actkind);
						ret.add(act);
					} else {
						// debug
						System.out.println("Not adding action "+act.getName());
					}
				}
				rs.close();
				d=d.getDomain();
			}
			stmt2.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve actions from database");			
	}
	
	public List<ActionParameter> getActionParameters(Action action)
	{
		//
		// Returns the action parameter list for a given action
		// (parameter name and type)
		//
		try
		{
			List<ActionParameter> ret = new ArrayList<ActionParameter>();
			String sql=	"SELECT		a.atname,a.attype,a.required " +
						"FROM		dm.dm_fragmentattrs		a,	" +
						"			dm.dm_fragments			b	" +
						"WHERE		b.actionid=?				" +
						"AND		b.id=a.typeid				" +
						"ORDER BY	a.atorder					";
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,action.getId());
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				String reqd = rs.getString(3);
				boolean rqd = (reqd != null && reqd.equalsIgnoreCase("Y"));
				ActionParameter p = new ActionParameter(rs.getString(1),rs.getString(2),rqd);
				ret.add(p);
			}
			return ret;	
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action parameter list from database");
	}
	
	// Deployment
	
	public boolean validateDeploymentId(int deployid)
	{
		boolean ret=false;
		try
		{
			String sql = "select count(*) from dm.dm_deployment where deploymentid=?";
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, deployid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int c = rs.getInt(1);
				ret = (c>0);
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve deployment " + deployid + " from database");	
	}
	
	public Deployment getDeployment(int deployid, boolean detailed)
	{
		String sql = null;
		if(detailed) {
			sql = "SELECT d.exitcode, d.finished, d.appid, a.name, d.envid, e.name, d.userid, u.name, u.realname, d.started, d.exitstatus "
				+ "FROM dm.dm_deployment d, dm.dm_application a, dm.dm_environment e, dm.dm_user u WHERE d.deploymentid = ? "
				+ "AND d.appid = a.id AND d.envid = e.id AND d.userid = u.id";
		} else {
			sql = "SELECT d.exitcode, d.finished FROM dm.dm_deployment d WHERE d.deploymentid = ?";
		}

		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, deployid);
			ResultSet rs = stmt.executeQuery();
			Deployment ret = null;
			if(rs.next()) {
				ret = new Deployment(this, deployid, rs.getInt(1));
				ret.setFinished(rs.getInt(2));
				if(detailed) {
					ret.setApplication(new Application(this, rs.getInt(3), rs.getString(4)));
					ret.setEnvironment(new Environment(this, rs.getInt(5), rs.getString(6)));
					User user = new User(this, rs.getInt(7), rs.getString(8));
					user.setRealName(rs.getString(9));
					ret.setUser(user);
					ret.setStarted(rs.getInt(10));
					ret.setExitStatus(rs.getString(11));
				}
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve deployment " + deployid + " from database");			
	}
	
	
	/**
	 * Look for a deployment associated with the current session.  Keep looking
	 * for "timeout" seconds.  Returns null if not found.
	 * @param sessionid
	 * @param timeout
	 * @return
	 */
	public Deployment getDeploymentBySessionId(TaskDeploy taskDeploy,int timeout)
	{
		String sessionid = taskDeploy.getDeploymentSessionId();
		String sql = "SELECT d.deploymentid, d.exitcode, d.finished FROM dm.dm_deployment d WHERE d.sessionid = ?";
		boolean EngineStopped = false;
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setString(1, sessionid);
			
			Deployment ret = null;
			while((ret == null) && (timeout > 0)) {
				ResultSet rs = stmt.executeQuery();
				if(rs.next()) {
					ret = new Deployment(this, rs.getInt(1), rs.getInt(2));
					ret.setFinished(rs.getInt(3));
				}
				rs.close();
				if(ret == null) {
					timeout--;
					try {
						if (!EngineStopped) {
							Thread.sleep(1000);
						}
						if (!taskDeploy.engineRunning()) {
							EngineStopped = true;
						}
					} catch (InterruptedException e) {}
				}	
			}
			if (EngineStopped && ret == null) {
				System.out.println("Engine has ended!");
				ret = new Deployment(this, -1, 1);
				ret.setFinished((int)timeNow());
				// taskDeploy.getErrorText();
				ret.setSummary(taskDeploy.getLastOutputLine());
			}
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}
	
	public List<Deployment.DeploymentLogEntry> getDeploymentLog(Deployment dep)
	{
		// TODO: remove l.runtime once lineno is working properly
		String sql = "SELECT l.lineno, l.stream, l.thread, l.line FROM dm.dm_deploymentlog l WHERE l.deploymentid = ? ORDER BY l.lineno, l.runtime";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, dep.getId());
			ResultSet rs = stmt.executeQuery();
			List<Deployment.DeploymentLogEntry> ret = new ArrayList<Deployment.DeploymentLogEntry>();
			while(rs.next()) {
				ret.add(dep.new DeploymentLogEntry(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4)));
			}
			rs.close();
			stmt.close();
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve deployment log " + dep.getId() + " from database");			
	}
	
	public List<Deployment.DeploymentLogEntry> getDeploymentLogSinceLine(Deployment dep, int lineno)
	{
		String sql = "SELECT l.lineno, l.stream, l.thread, l.line FROM dm.dm_deploymentlog l WHERE l.deploymentid = ? AND l.lineno > ? ORDER BY l.lineno";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, dep.getId());
			stmt.setInt(2, lineno);
			ResultSet rs = stmt.executeQuery();
			List<Deployment.DeploymentLogEntry> ret = new ArrayList<Deployment.DeploymentLogEntry>();
			while(rs.next()) {
				ret.add(dep.new DeploymentLogEntry(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4)));
			}
			rs.close();
			stmt.close();
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve deployment log " + dep.getId() + " from database");			
	}
	
	public List<Deployment.DeploymentXfer> getDeploymentXfers(Deployment dep)
	{
		String sql = "SELECT x.stepid, x.repoid, x.reponame, x.repoinstanceid, x.repopath, x.repover, "
			+ "x.componentid, x.componentname, x.serverid, x.servername, x.targetfilename, x.checksum2, x.buildnumber "
			+ "FROM dm.dm_deploymentxfer x WHERE x.deploymentid = ? ORDER BY x.stepid, x.repopath";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, dep.getId());
			ResultSet rs = stmt.executeQuery();
			List<Deployment.DeploymentXfer> ret = new ArrayList<Deployment.DeploymentXfer>();
			while(rs.next()) {
				Component comp = null;
				int compid = getInteger(rs, 7, 0);
				if(compid != 0) {
					comp = new Component(this, compid, rs.getString(8));
				}
				ret.add(dep.new DeploymentXfer(rs.getInt(1),
						new Repository(this, rs.getInt(2), rs.getString(3)),
						rs.getInt(4), rs.getString(5), rs.getString(6), comp,
						new Server(this, rs.getInt(9), rs.getString(10)),
						rs.getString(11), rs.getString(12), getInteger(rs,13,0)));
			}
			rs.close();
			stmt.close();
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve deployment xfers " + dep.getId() + " from database");			
	}
	
	public List<Deployment.DeploymentScript> getDeploymentScripts(Deployment dep)
	{
		String sql = "SELECT stepid,actionid FROM dm.dm_deploymentactions WHERE deploymentid = ? ORDER BY stepid";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, dep.getId());
			ResultSet rs = stmt.executeQuery();
			List<Deployment.DeploymentScript> ret = new ArrayList<Deployment.DeploymentScript>();
			while(rs.next()) {
				Action action = null;
				int actionid = getInteger(rs, 2, 0);
				if(actionid != 0) {
					action = getAction(actionid,true);
				}
				ret.add(dep.new DeploymentScript(rs.getInt(1),action));
			}
			rs.close();
			stmt.close();
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve deployment xfers " + dep.getId() + " from database");			
	}
	
	public PropertyDataSet getDeploymentProps(Deployment dep, int stepid, int instid)
	{
		String sql = "SELECT p.name, p.value FROM dm.dm_deploymentprops p "
			+ "WHERE p.deploymentid = ? AND p.stepid = ? AND p.instanceid = ? ORDER BY 1";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, dep.getId());
			stmt.setInt(2, stepid);
			stmt.setInt(3, instid);
			ResultSet rs = stmt.executeQuery();
			PropertyDataSet ret = new PropertyDataSet();
			while(rs.next()) {
				ret.addProperty(rs.getString(1), rs.getString(2));
			}
			rs.close();
			stmt.close();
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve deployment props " + dep.getId() + ":" + stepid + ":" + instid + " from database");			
	}
	
	public ReportDataSet getTimePerStepForDeployment(int deployid)
	{
		String sql = "SELECT 'Step '||s.stepid||': '||s.type||' ('||concat(s.finished - s.started,' secs')||')', (s.finished - s.started) + 1 FROM dm.dm_deploymentstep s WHERE s.deploymentid = ? ORDER BY s.stepid DESC";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, deployid);
			ResultSet rs = stmt.executeQuery();
			ReportDataSet ret = new ReportDataSet(rs);
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve deployment time report from database");			
	}
	
	public GanttDataSet getDeploymentStepsGantt(int deployid)
	{
		GanttDataSet ret = new GanttDataSet();
		
		String sql = "SELECT s.stepId, s.type, s.started, s.finished FROM dm.dm_deploymentstep s WHERE s.deploymentid = ? ORDER BY s.stepid";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, deployid);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				ret.addStep(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4));
			}
			rs.close();
			stmt.close();
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
			throw new RuntimeException("Unable to retrieve deployment step gantt from database");
		}
		
		String sql2 = "SELECT s.stepId, c.serverid, c.servername, s.started, s.finished FROM dm.dm_deploymentscript c, dm.dm_deploymentstep s "
				+ "WHERE s.deploymentid = ? AND c.deploymentid = s.deploymentid AND c.stepid = s.stepid ORDER BY s.stepid";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql2);
			stmt.setInt(1, deployid);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				ret.addServerStep(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5));
			}
			rs.close();
			stmt.close();
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
			throw new RuntimeException("Unable to retrieve deployment step gantt from database");
		}
		
		String sql3 = "SELECT DISTINCT s.stepId, x.serverid, x.servername, s.started, s.finished FROM dm.dm_deploymentxfer x, dm.dm_deploymentstep s "
				+ "WHERE s.deploymentid = ? AND x.deploymentid = s.deploymentid AND x.stepid = s.stepid ORDER BY s.stepid";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql3);
			stmt.setInt(1, deployid);
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
				ret.addServerStep(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getInt(5));
			}
			rs.close();
			stmt.close();
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
			throw new RuntimeException("Unable to retrieve deployment step gantt from database");
		}

		return ret;
	}

	
	// Object
	
	public DMObject getObject(ObjectType objtype, int id)
	{
		return getObject(objtype, id, false);
	}
	
	public DMObject getDetailedObject(ObjectType objtype, int id)
	{
		return getObject(objtype, id, true);
	}
	
	private DMObject getObject(ObjectType objtype, int id, boolean detailed)
	{
		switch(objtype) {
		case USER:
			System.out.println("getObject returning USER");
			return getUser(id);
		case USERGROUP:
			System.out.println("getObject returning USERGROUP");
			return getGroup(id);
		case DOMAIN:
			System.out.println("getObject returning DOMAIN");
			return getDomain(id);
		case RELEASE:
		case APPLICATION:
		case APPVERSION:
		case RELVERSION: 
			return getApplication(id, detailed);
		case COMPONENT:
		case COMPVERSION:
			return getComponent(id, detailed);
		case COMPONENTITEM:
			return getComponentItem(id, detailed);
		case CREDENTIALS:
			return getCredential(id, detailed);
		case ACTION:
		case FUNCTION:   
		case PROCEDURE:
			return getAction(id, detailed,objtype);
		case DEPLOYMENT:
			return getDeployment(id, detailed);
		case ENVIRONMENT:
			return getEnvironment(id, detailed);
		case SERVER:
			return getServer(id, detailed);
		case TASK:
			return getTask(id, detailed);
		case ENGINE:
			return getEngine(id);
		case REPOSITORY:
		case DATASOURCE:
		case NOTIFY:
			return getProviderObject(objtype, id, detailed);
		case SERVERCOMPTYPE:
			return getServerCompTypeDetail(id);	
		case TEMPLATE:
			return getTemplate(id);
		case BUILDER:
			return getBuilder(id);
		case BUILDJOB:
			return getBuildJob(id);
		default:
			throw new RuntimeException("Unknown provider object type " + objtype);
		}		
	}
	
	public boolean isValidDomainForObject(DMObject obj,boolean inherit)
	{
		String sql = null;
		
		if (obj.getId() < 0)
		 return true;
		
		if (obj.getObjectType() == ObjectType.TEMPLATE) {
			// Templates don't have domains - but their parent notify process does
			sql = "SELECT a.domainid FROM dm.dm_notify a,dm.dm_template b WHERE a.id = b.notifierid AND b.id=?";
		}
		else if (obj.getObjectType() == ObjectType.BUILDJOB) {
			// Build Jobs don't have domains - but their parent build engine does
			sql = "SELECT a.domainid FROM dm.dm_buildengine a,dm.dm_buildjob b WHERE a.id = b.builderid AND b.id=?";
		} else {
			sql = "SELECT o.domainid FROM dm." + obj.getDatabaseTable() + " o WHERE o.id = ?";
		}
		try
		{
			System.out.println("isValidDomainForObject, sql="+sql+" id="+obj.getId());
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, obj.getId());
			ResultSet rs = stmt.executeQuery();
			boolean ret = false;
			if(rs.next()) {
				int domainid = getInteger(rs, 1, 0);
				System.out.println("object domain is "+domainid);
				if (obj.getObjectType() == ObjectType.DOMAIN) {
					// our home domain is always valid
					if (obj.getId() == m_userDomain) {
						ret = true;
					} else {
						ret = (domainid != 0) ? ValidDomain(domainid,inherit) : true;	// top-level domain is always true
					}
				} else {
					ret = (domainid != 0) ? ValidDomain(domainid,inherit) : true;	// top-level domain is always true
				}
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve domain for " + obj.getClass().getName() + " " + obj.getId() + " from database");				
	}
	
	public boolean isValidDomainForObject(DMObject obj)
	{
		return isValidDomainForObject(obj,false);
	}
	
	private void addAccessForDomain(int domainid,boolean recursing,Hashtable<Integer, ObjectAccess> ia)
	{
		//
		// Create HashList of Group, ObjectAccess
		// all "inherit" flags to false
		// recurse to:
		// a) Grab list of groups and access flags from domaininherit table
		// b) Look up corresponding group in hash table
		// c) If present in hash then set any flag with the inherit flag false and set the flag to true
		//    If NOT present, add a new entry to the hash table, inherit flag true for actual values, false for NULL values
		// d) Recurse to (a) with parent domain
		//
//		System.out.println("**** addAccessForDomain("+domainid+")");
		
		// Need to change this because domaininherit is no longer populated
		// String sql = 	"SELECT a.usrgrpid, a.viewaccess, a.updateaccess,a.readaccess,a.writeaccess,b.domainid	"
		// 		+		"FROM dm.dm_domaininherit a,dm.dm_domain b WHERE a.domainid=? AND b.id=a.domainid";
		String sql = 	"SELECT a.usrgrpid, a.viewaccess, a.updateaccess,a.readaccess,a.writeaccess,b.domainid	"
		 		+		"FROM dm.dm_domainaccess a, dm.dm_domain b WHERE a.domainid=? AND b.id=a.domainid";
		try
		{
//			System.out.println("sql="+sql);
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,domainid);
			ResultSet rs = stmt.executeQuery();
			int parentdomain=0;
			int rows=0;
			while(rs.next()) {
	//			System.out.println("got a row");
				rows++;
				int groupid = rs.getInt(1);
				parentdomain = getInteger(rs,6,0);
	//			System.out.println("groupid="+groupid+" parentdomain="+parentdomain);
				
				if (ia.containsKey(groupid)) {
					// This group already exists - set the access for all non-null values
					//System.out.println("Group "+groupid+" already exists in hash");
					ObjectAccess eoa = ia.get(groupid);
					
					//eoa.SetAccess(rs,2,true,recursing);
					if(recursing) {	// recursing being false indicates displaying inherited permissions tab on domain
						eoa.addDomainAccess(getString(rs, 2, null), getString(rs, 3, null), getString(rs, 4, null), getString(rs, 5, null));
					} else {
						eoa.addObjectAccess(getString(rs, 2, null), getString(rs, 3, null), getString(rs, 4, null), getString(rs, 5, null));						
					}
					
					//ia.put(groupid,eoa); - RHT - this is unnecessary - get does not remove the entry
				} else {
					// New group
					//System.out.println("New group");
					ObjectAccess oa = new ObjectAccess();
					//oa.SetAccess(rs,2,true,recursing);
					if(recursing) {	// recursing being false indicates displaying inherited permissions tab on domain
						oa.addDomainAccess(getString(rs, 2, null), getString(rs, 3, null), getString(rs, 4, null), getString(rs, 5, null));
					} else {
						oa.addObjectAccess(getString(rs, 2, null), getString(rs, 3, null), getString(rs, 4, null), getString(rs, 5, null));						
					}
					ia.put(groupid,oa);
					// debug
					//System.out.println("added new group, hashtable content:");
					//Enumeration<Integer> enumKey = ia.keys();
					//while (enumKey.hasMoreElements()) {
					//    Integer gid = enumKey.nextElement();
					//    ObjectAccess oax = ia.get(gid);
					//    System.out.println("Group "+gid+" "+(oax.isReadable()?"Y":"N") + " " + (oax.isWriteable()?"Y":"N") + " " + (oax.isViewable()?"Y":"N")+ " " + (oax.isUpdatable()?"Y":"N"));
					//    System.out.println("INHER "+gid+" "+(oax.isReadInherited()?"Y":"N") + " " + (oax.isWriteInherited()?"Y":"N") + " " + (oax.isViewInherited()?"Y":"N")+ " " + (oax.isUpdateInherited()?"Y":"N"));
					//}
					// end-debug
				}
			}
			if (rows==0) {
				// Nothing was retrieved - parentid will not be set. Retrieve it here
				String psql="SELECT domainid FROM dm.dm_domain WHERE id =?";
				PreparedStatement pstmt = getDBConnection().prepareStatement(psql);
				pstmt.setInt(1,domainid);
				ResultSet prs = pstmt.executeQuery();
				if (prs.next()) {
					parentdomain = prs.getInt(1);
				}
				prs.close();
				pstmt.close();
			}
			rs.close();
			stmt.close();
			// Okay, now recurse up to the parent domain
			if (parentdomain>0) {
				// System.out.println("Recursing...");
				addAccessForDomain(parentdomain,true,ia);
			}
			
			// debug
			//System.out.println("About to exit, hashtable content:");
			//Enumeration<Integer> enumKey = ia.keys();
			//while (enumKey.hasMoreElements()) {
			//    Integer groupid = enumKey.nextElement();
			//    ObjectAccess oa = ia.get(groupid);
			//    System.out.println("Group "+groupid+" "+(oa.isReadable()?"Y":"N") + " " + (oa.isWriteable()?"Y":"N") + " " + (oa.isViewable()?"Y":"N")+ " " + (oa.isUpdatable()?"Y":"N"));
			//    System.out.println("      "+groupid+" "+(oa.isReadInherited()?"Y":"N") + " " + (oa.isWriteInherited()?"Y":"N") + " " + (oa.isViewInherited()?"Y":"N")+ " " + (oa.isUpdateInherited()?"Y":"N"));
			//}
			//System.out.println("**** exit addAccessForDomain");
			// end-debug
			
			return;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve inheritance access control list for domain "+ domainid + " from database");				
	}
	
	public Hashtable<Integer, ObjectAccess> getAccessForDomain(int domainid)
	{
		Hashtable<Integer, ObjectAccess> ret = new Hashtable<Integer, ObjectAccess>();
		addAccessForDomain(domainid,false,ret);
		return ret;
	}
	
	public Hashtable<Integer, ObjectAccess> getAccessForObject(DMObject obj, boolean forDisplay)
	{
		String sql;
		if (obj.getObjectType() == ObjectType.TEMPLATE) {
			sql = "SELECT a.usrgrpid, a.viewaccess, a.updateaccess,a.readaccess,a.writeaccess "
					+ "FROM dm.dm_notifyaccess a,dm.dm_template b WHERE a.notifyid = b.notifierid AND b.id = ?";
		} else 
		if (obj.getObjectType() == ObjectType.BUILDJOB) {
			sql = "SELECT a.usrgrpid, a.viewaccess, a.updateaccess,a.readaccess,a.writeaccess "
					+ "FROM dm.dm_buildengineaccess a,dm.dm_buildjob b WHERE a.builderid = b.builderid AND b.id = ?";
		} else {
			if (obj.hasReadWrite()) {
				sql = "SELECT a.usrgrpid, a.viewaccess, a.updateaccess,a.readaccess,a.writeaccess "
				+ "FROM dm." + obj.getDatabaseTable() + "access a WHERE a." + obj.getForeignKey() + " = ?";
			} else {
				sql = "SELECT a.usrgrpid, a.viewaccess, a.updateaccess "
				+ "FROM dm." + obj.getDatabaseTable() + "access a WHERE a." + obj.getForeignKey() + " = ?";
			}
		}
		
		System.out.println("getAccessForObject("+obj.getName()+","+forDisplay+")");
		System.out.println("SQL="+sql);
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, obj.getId());
			ResultSet rs = stmt.executeQuery();
			Hashtable<Integer, ObjectAccess> ret = new Hashtable<Integer, ObjectAccess>();
			while(rs.next()) {	
				System.out.println("Got a row");
				if (obj.hasReadWrite()) {
					ObjectAccess oa = new ObjectAccess();
					//oa.SetAccess(rs,2,true,false);
					oa.addObjectAccess(getString(rs, 2, null), getString(rs, 3, null), getString(rs, 4, null), getString(rs, 5, null));
					ret.put(rs.getInt(1), oa);
				} else {
					ObjectAccess oa = new ObjectAccess();
					oa.addObjectAccess(getString(rs, 2, null), getString(rs, 3, null));
					//oa.SetAccess(rs,2,false,false);
					ret.put(rs.getInt(1), oa);
				}
			}
			// System.out.println("End of object permission read - hashtable is:");
			addAccessForDomain(obj.getDomainId(),true,ret);
			/*
			Enumeration<Integer> enumKey = ret.keys();
			while (enumKey.hasMoreElements()) {
			    Integer groupid = enumKey.nextElement();
			    ObjectAccess oa = ret.get(groupid);
			    System.out.println("Group "+groupid+" "+(oa.isReadable()?"Y":"N") + " " + (oa.isWriteable()?"Y":"N") + " " + (oa.isViewable()?"Y":"N")+ " " + (oa.isUpdatable()?"Y":"N"));
			    System.out.println("      "+groupid+" "+(oa.isReadInherited()?"Y":"N") + " " + (oa.isWriteInherited()?"Y":"N") + " " + (oa.isViewInherited()?"Y":"N")+ " " + (oa.isUpdateInherited()?"Y":"N"));
			    System.out.println("      "+groupid+" "+(oa.isReadDenied()?"Y":"N") + " " + (oa.isWriteDenied()?"Y":"N") + " " + (oa.isViewDenied()?"Y":"N")+ " " + (oa.isUpdateDenied()?"Y":"N"));
			}
			*/
			rs.close();
			stmt.close();
			

			// This bit gives super-users access to change everything within the domains they can see
			if(!forDisplay && m_OverrideAccessControl) {
				ObjectAccess oa = ret.get(UserGroup.EVERYONE_ID);
				if(oa != null) {
					System.out.println("User is SUPERUSER - attempting to grant access by modifying EVERYONE");
					System.out.println("Writeable is "+oa.isWriteable());
					oa.addObjectAccess("Y", "Y", (oa.isReadable() ? "Y" : "N"), (oa.isWriteable() ? "Y" : "N"));
				} else {
					System.out.println("User is SUPERUSER - attempting to grant access by adding EVERYONE");
					oa = new ObjectAccess();
					oa.addObjectAccess("Y", "Y");	// only view and update - still can't read and write
					ret.put(UserGroup.EVERYONE_ID, oa);
				}
				return ret;
			}
			
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve access control list for " + obj.getClass().getName() + " " + obj.getId() + " from database");				
	}
	
	public List<DMAttribute> getAttributesForObject(DMObject obj)
	{
		String sql = "SELECT v.name, v.value, v.arrayid, v.nocase "
			+ "FROM dm." + obj.getDatabaseTable() + "vars v WHERE v." + obj.getForeignKey() + " = ? ORDER BY 1";
		System.out.println("In getAttributesForObject - sql = "+sql);
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, obj.getId());
			ResultSet rs = stmt.executeQuery();
			List<DMAttribute> ret = new ArrayList<DMAttribute>();
			while(rs.next()) {
				System.out.println("Got a row, name="+rs.getString(1)+" val="+rs.getString(2));
				int arrayid = rs.getInt(3);
				String name = rs.getString(1);
				
				if(arrayid > 0) {
				 String sql2 = "select name, value from dm.dm_arrayvalues where id = ?";
		   PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
		   stmt2.setInt(1, arrayid);
		   ResultSet rs2 = stmt2.executeQuery();	
		   while(rs2.next()) {		   
				 	ret.add(new DMAttribute(name, arrayid, rs2.getString(1), rs2.getString(2)));
		   }	
		   rs2.close();
		   stmt2.close();
				} else {
					ret.add(new DMAttribute(rs.getString(1), rs.getString(2)));
				}
			}
			System.out.println("Finished retrieving rows");
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve attributes for " + obj.getClass().getName() + " " + obj.getId() + " from database");				
	}
	
	public List<DMAttribute> getArrayAttributes(int arrid)
	{
		String sql = "SELECT a.name, a.value FROM dm.dm_arrayvalues a WHERE a.id = ? ORDER BY 1";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, arrid);
			ResultSet rs = stmt.executeQuery();
			List<DMAttribute> ret = new ArrayList<DMAttribute>();
			while(rs.next()) {
				ret.add(new DMAttribute(rs.getString(1), rs.getString(2)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve array values for array " + arrid + " from database");				
	}

	/*
	private boolean internalUpdateArray(int dataid, int arrayid, AttributeChangeSet changes)
			throws SQLException
	{
		System.out.println("internalUpdateArray - " + dataid + "; " + arrayid);
		String dsql = "DELETE FROM dm.dm_arrayvalues av WHERE av.id = ? AND av.name = ?";
		String asql = "INSERT INTO dm.dm_arrayvalues(id,name,value) VALUES(?,?,?)";
		String csql = "UPDATE dm.dm_arrayvalues av SET value = ? WHERE av.id = ? AND av.name = ?";
		PreparedStatement stmt = m_conn.prepareStatement(dsql);
		for(DMAttribute a : changes.deletedElements(dataid)) {
			System.out.println("Deleting '" + a.getName() + "' from array " + arrayid);
			stmt.setInt(1, arrayid);
			stmt.setString(2, a.getName());
			stmt.execute();			
		}
		stmt.close();
		
		stmt = m_conn.prepareStatement(asql);
		for(DMAttribute a : changes.addedElements(dataid)) {
			System.out.println("Inserting '" + a.getName() + "' into array " + arrayid);
			stmt.setInt(1, arrayid);
			stmt.setString(2, a.getName());
			stmt.setString(3, a.getValue());
			stmt.execute();			
		}
		stmt.close();
		
		stmt = m_conn.prepareStatement(csql);
		for(DMAttribute a : changes.changedElements(dataid)) {
			System.out.println("Updating '" + a.getName() + "' in array " + arrayid);
			stmt.setString(1, a.getValue());
			stmt.setInt(2, arrayid);
			stmt.setString(3, a.getName());
			stmt.execute();			
		}
		stmt.close();
		return true;
	}
	*/
	
	private boolean internalUpdateAttributes(String table, String fk, int id, AttributeChangeSet changes)
	{
		//TODO: Need to update nocase column as well
		System.out.println("internalUpdateAttributes - " + table);
		String dsqla = "DELETE FROM dm." + table + "vars v WHERE v." + fk + " = ? AND v.name = ?";
		String dsqlb = "DELETE FROM dm.dm_arrayvalues av WHERE av.id = ? and av.name = ?";
		String asql1 = "INSERT INTO dm." + table + "vars(" + fk + ",name,value) VALUES(?,?,?)";
		String asql2 = "INSERT INTO dm." + table + "vars(" + fk + ",name,arrayid) VALUES(?,?,?)";
		String csql1 = "UPDATE dm." + table + "vars v SET name = ?, value = ? WHERE v." + fk + " = ? AND v.name = ?";
		try {

			for(DMAttribute a : changes.deleted()) {
				System.out.println("Deleting " + id + " '" + a.getName() + "' from " + table + "vars");
				if (a.isArray()) {
					// Delete any array values associated with this variable
					PreparedStatement stmt2 = getDBConnection().prepareStatement(dsqlb);
					stmt2.setInt(1, a.getArrayId());
					stmt2.setString(2, a.getKey());
					stmt2.execute();
					stmt2.close();
					
				   Statement st = getDBConnection().createStatement();
				   ResultSet rs = st.executeQuery("SELECT count(*) from dm.dm_arrayvalues where id =" + a.getArrayId());
				   rs.next();
		
				   int c = rs.getInt(1);
				   rs.close();
				   st.close();
		     
				   if (c == 0) { 
						PreparedStatement stmt = getDBConnection().prepareStatement(dsqla);
						stmt.setInt(1, id);
						stmt.setString(2, a.getName());
						stmt.execute();  
						stmt.close();
				   }
				}
				else
				{
				     PreparedStatement stmt = getDBConnection().prepareStatement(dsqla);
				     stmt.setInt(1, id);
				     stmt.setString(2, a.getName());
				     stmt.execute();  
				     stmt.close();
				}
			}

			for(DMAttribute a : changes.changed()) {
				System.out.println("Updating " + id + " '" + a.getName() + "' in " + table + "vars");

				if (a.isArray()) {
					String updateStr = "UPDATE dm.dm_arrayvalues set name = ?, value = ? where id = ? and name = ?";
					 
					PreparedStatement stmt2 = getDBConnection().prepareStatement(updateStr);
					stmt2.setString(1, a.getKey());
					stmt2.setString(2, a.getValue());     
					stmt2.setInt(3, a.getArrayId());
					stmt2.setString(4, a.getKey());
					stmt2.execute();
					stmt2.close();
				} 

				PreparedStatement stmt2 = getDBConnection().prepareStatement(csql1);  
				stmt2.setString(1, a.getName());
				stmt2.setString(2, a.getValue());
				stmt2.setInt(3, id);
				stmt2.setString(4, a.getName());
				stmt2.execute();
				if (stmt2.getUpdateCount() == 0) {
					// Nothing was updated - add to "add" list
					changes.addAdded(a);
				}
				stmt2.close(); 
			}
			
			for(DMAttribute a : changes.added()) {
				System.out.println("Inserting " + id + " '" + a.getName() + "' into " + table + "vars");
				
    String cntStr = "SELECT arrayid from dm." + table + "vars where " + fk + " = ? and name = ?";
    
    PreparedStatement stmt2 = getDBConnection().prepareStatement(cntStr);
    stmt2.setInt(1, id);
    stmt2.setString(2, a.getName());     
    
    int arrayid = -1;
    
    ResultSet rs = stmt2.executeQuery( );

    if (rs.next())
    {
     arrayid = rs.getInt(1);
     if (arrayid == 0)
      arrayid = -1;
    } 
    
    rs.close();
    stmt2.close();
    
    if (arrayid == -1)
    { 
     if (a.isArray())
     {
      arrayid = getID("arrayvalues");
      PreparedStatement stmt = getDBConnection().prepareStatement(asql2);
      stmt.setInt(1, id);
      stmt.setString(2, a.getName());
      stmt.setInt(3, arrayid);
      stmt.execute();
      stmt.close();
      
      stmt = getDBConnection().prepareStatement("INSERT into dm.dm_arrayvalues(id,name,value) values(?,?,?)");
      stmt.setInt(1, arrayid);
      stmt.setString(2, a.getKey());
      stmt.setString(3, a.getValue());
      stmt.execute();
      stmt.close();
     }
     else
     {
      PreparedStatement stmt = getDBConnection().prepareStatement(asql1);
      stmt.setInt(1, id);
      stmt.setString(2, a.getName());
      stmt.setString(3, a.getValue());
      stmt.execute();
      stmt.close();     
     }
    }
    else
    {
     if (a.isArray())
     { 
      PreparedStatement stmt = getDBConnection().prepareStatement("INSERT into dm.dm_arrayvalues(id,name,value) values(?,?,?)");
      stmt.setInt(1, arrayid);
      stmt.setString(2, a.getKey());
      stmt.setString(3, a.getValue());
      stmt.execute();
      stmt.close();
     } 
    }
			}
			getDBConnection().commit();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}

	public boolean updateAttributesForObject(DMObject obj, AttributeChangeSet changes)
	{
		updateModTime(obj);
		RecordObjectUpdate(obj,"Attributes Changed");
		return internalUpdateAttributes(obj.getDatabaseTable(), obj.getForeignKey(), obj.getId(), changes);
	}

	
	///////////////////////////////////////////////////////////////////////////
	// ProviderObject
	
	public ProviderObject getProviderObject(ObjectType objtype, int id, boolean detailed)
	{
		switch(objtype) {
		case REPOSITORY: return getRepository(id, detailed);
		case DATASOURCE: return getDatasource(id, detailed);
		case NOTIFY: return getNotify(id, detailed);
		case BUILDER: return getBuilder(id);
		default: throw new RuntimeException("Unknown provider object type");
		}
	}
	
	
	public boolean updateProviderObject(ProviderObject po, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm." + po.getDatabaseTable() + " ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case PROVIDER_TYPE: {				
				if(po.getDef().getId() != ProviderDefinition.UNCONFIGURED_ID) {
					throw new RuntimeException("Provider type cannot be changed after creation");
				}
				DMObject def = (DMObject) changes.get(field);
				if(def == null) {
					throw new RuntimeException("Provider type must be specified");					
				}
				update.add(", defid = ?", def.getId());
				}
				break;
			case PROVIDER_CRED: {
				DMObject cred = (DMObject) changes.get(field);
				update.add(", credid = ?", (cred != null) ? cred.getId() : Null.INT);
				}
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(po, update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", po.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	/*
	private boolean getEncrypted(String PropertyTable,String fk,int id,String fieldName) throws SQLException
	{
		boolean res = false;
		PreparedStatement stmt = m_conn.prepareStatement("SELECT encrypted FROM dm."+PropertyTable+" WHERE "+fk+"=? AND name=?");
		stmt.setInt(1,id);
		stmt.setString(2,fieldName);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			res = rs.getString(1).equalsIgnoreCase("y");
		}
		rs.close();
		stmt.close();
		return res;
	}
	*/
	
	private boolean internalUpdateProperties(String table, String fk, int id, Domain dom, ACDChangeSet<DMProperty> changes)
	{
		System.out.println("internalUpdateProperties - " + table);
		boolean repchange = (table.equalsIgnoreCase("dm_repository"));
		String dsql = "DELETE FROM dm." + table + "props p WHERE p." + fk + " = ? AND p.name = ?";
		String asql = "INSERT INTO dm." + table + "props(" + fk + ",name,value,encrypted,overridable,appendable) VALUES(?,?,?,?,?,?)";
		String csql = "UPDATE dm." + table + "props p SET value = ?, encrypted = ?, overridable = ?, appendable = ? WHERE p." + fk + " = ? AND p.name = ?";
		String isql = "DELETE FROM dm.dm_compitemprops WHERE name=? AND compitemid in (SELECT id FROM dm.dm_componentitem WHERE repositoryid=?)";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(dsql);
			for(DMProperty p : changes.deleted()) {
				System.out.println("Deleting " + id + " '" + p.getName() + "' from " + table + "props");
				stmt.setInt(1, id);
				stmt.setString(2, p.getName());
				stmt.execute();
			}
			stmt.close();
			
			stmt = getDBConnection().prepareStatement(asql);
			for(DMProperty p : changes.added()) {
				System.out.println("Inserting " + id + " '" + p.getName() + "' into " + table + "props");
				stmt.setInt(1, id);
				stmt.setString(2, p.getName());
				if (p.isEncrypted()) {
					// Encrypted value - use engine to encrypt
					Engine eng = (dom != null) ? dom.findNearestEngine() : null;
					if(eng == null) {
						System.err.println("ERROR: Could not find engine to encrypt data");
					}
					stmt.setString(3,eng.encryptValue(p.getValue(), GetUserName())); // throws runtime exception on failure
				} else {
					stmt.setString(3, p.getValue());
				}
				stmt.setString(4, p.isEncrypted() ? "Y" : "N");
				stmt.setString(5, p.isOverridable() ? "Y" : "N");
				stmt.setString(6, p.isAppendable() ? "Y" : "N");
				stmt.execute();
				if (repchange) {
					if (!p.isOverridable() && !p.isAppendable()) {
						// Not Overridable and not Appendable
						// Remove any component item property referencing this attribute
						PreparedStatement stmt2 = getDBConnection().prepareStatement(isql);
						stmt2.setString(1,p.getName());
						stmt2.setInt(2,id);
						stmt2.execute();
					}
				}
			}
			stmt.close();

			stmt = getDBConnection().prepareStatement(csql);
			for(DMProperty p : changes.changed()) {
				System.out.println("Updating " + id + " '" + p.getName() + "' in " + table + "props, getValue()="+p.getValue());
				// boolean oldEncrypted = getEncrypted(table+"props",fk,id,p.getName());
				if (p.isEncrypted()) {
					Engine eng = (dom != null) ? dom.findNearestEngine() : null;
					if(eng == null) {
						System.err.println("ERROR: Could not find engine to encrypt data");
					}
					stmt.setString(1,eng.encryptValue(p.getValue(), GetUserName())); // throws runtime exception on failure
				}
				else
				{
					stmt.setString(1, p.getValue());
				}
				stmt.setString(2, p.isEncrypted() ? "Y" : "N");
				stmt.setString(3, p.isOverridable() ? "Y" : "N");
				stmt.setString(4, p.isAppendable() ? "Y" : "N");
				stmt.setInt(5, id);
				stmt.setString(6, p.getName());
				stmt.execute();
				if (repchange) {
					if (!p.isOverridable() && !p.isAppendable()) {
						// Not Overridable and not Appendable
						// Remove any component item property referencing this attribute
						PreparedStatement stmt2 = getDBConnection().prepareStatement(isql);
						stmt2.setString(1,p.getName());
						stmt2.setInt(2,id);
						stmt2.execute();
					}
				}
			}
			stmt.close();

			getDBConnection().commit();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		} catch(RuntimeException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}


	public boolean updateProviderProperties(ProviderObject po, ACDChangeSet<DMProperty> changes)
	{
		return internalUpdateProperties(po.getDatabaseTable(), po.getForeignKey(), po.getId(), po.getDomain(),changes);
	}
	
	
	public ProviderDefinition getProviderDefForProviderObject(ProviderObject po)
	{
		String sql = "SELECT d.id, d.name FROM dm.dm_providerdef d, dm."
			+ po.getDatabaseTable() + " p WHERE p.id = ? AND d.id = p.defid";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, po.getId());
			ResultSet rs = stmt.executeQuery();
			ProviderDefinition ret = null;
			if(rs.next()) {
				ret = new ProviderDefinition(this, rs.getInt(1), rs.getString(2));
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve def for " + po.getClass().getName() + " " + po.getId() + " from database");				
	}
	
	public List<DMProperty> getPropertiesForProviderObject(ProviderObject po)
	{
		String sql = "SELECT p.name, p.value, p.encrypted, p.overridable, p.appendable "
			+ "FROM dm." + po.getDatabaseTable() + "props p WHERE p." + po.getForeignKey() + " = ? ORDER BY 1";	
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, po.getId());
			ResultSet rs = stmt.executeQuery();
			List<DMProperty> ret = new ArrayList<DMProperty>();
			while(rs.next()) {
				ret.add(new DMProperty(rs.getString(1), rs.getString(2), getBoolean(rs, 3), getBoolean(rs, 4), getBoolean(rs, 5)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve properties for " + po.getClass().getName() + " " + po.getId() + " from database");				
	}
	
		
	// ProviderDefinition
	
	public ProviderDefinition getProviderDefinition(int defid)
	{
		String sql = "SELECT pd.id, pd.name, pd.kind FROM dm.dm_providerdef pd WHERE pd.id = ?";	
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, defid);
			ResultSet rs = stmt.executeQuery();
			ProviderDefinition ret = null;
			if(rs.next()) {
				ret = new ProviderDefinition(this, rs.getInt(1), rs.getString(2));
				ret.setKind(ObjectType.fromInt(rs.getInt(3)));
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve provider definition " + defid + " from database");				
	}

	
	public List<ProviderDefinition> getProviderDefinitionsOfType(ObjectType type)
	{
		String sql = "SELECT pd.id, pd.name FROM dm.dm_providerdef pd WHERE pd.kind = ? ORDER BY 2";	
		System.out.println("getProviderDefinitionsOfType type="+type.value());
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, type.value());
			ResultSet rs = stmt.executeQuery();
			List<ProviderDefinition> ret = new ArrayList<ProviderDefinition>();
			while(rs.next()) {
				ProviderDefinition pd = new ProviderDefinition(this, rs.getInt(1), rs.getString(2));
				pd.setKind(type);
				ret.add(pd);
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve provider definitions of type " + type + " from database");				
	}
	
	private List<Datasource> internalGetDataSource(String sql2,int domid)
	{
		System.out.println("internalGetDataSource(domid="+domid+")");
		Domain d = getDomain(domid);
		List<Datasource> ret = new ArrayList<Datasource>();
		Hashtable<Integer,char[]> accessRights = new Hashtable<Integer,char[]>();
		try
		{
			String sql1 = "select a.id,b.viewaccess,b.readaccess	"
					+	"from	dm.dm_datasource		a,	"
					+	"		dm.dm_datasourceaccess	b,	"
					+	"		dm.dm_usersingroup		c	"
					+	"where	c.userid=?					"
					+	"and	c.groupid=b.usrgrpid		"
					+	"and	a.id=b.datasourceid				"
					+	"and	a.domainid=?				"
					+	"union	"
					+	"select	d.id,e.viewaccess,e.readaccess	"
					+	"from	dm.dm_datasource		d,	"
					+	"		dm.dm_datasourceaccess	e	"
					+	"where	e.usrgrpid=1			"	// user group 1 = Everyone
					+	"and	d.id=e.datasourceid			"
					+	"and	d.domainid=?			";
			
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			while (d != null && d.getId()>=1) {
				System.out.println("domain is "+d.getName()+") read="+d.isReadable(true)+" write="+d.isWriteable(true)+" update="+d.isUpdatable(true)+" view="+d.isViewable(true));
				if (!m_OverrideAccessControl) {
					// Get a list of actions in this domain with non-default access permissions
					System.out.println("no override access, getting list of overrides userid="+getUserID()+" domainid="+d.getId());
					stmt1.setInt(1,getUserID());
					stmt1.setInt(2,d.getId());
					stmt1.setInt(3,d.getId());
					ResultSet rs = stmt1.executeQuery();
					while (rs.next()) {
						char[] ar = new char[2];
						ar[0] = getString(rs,2,"-").charAt(0);
						ar[1] = getString(rs,3,"-").charAt(0);
						System.out.println("id="+rs.getInt(1)+" ar[0]="+ar[0]+" ar[1]="+ar[1]);
						accessRights.put(rs.getInt(1),ar);
					}
					rs.close();
				}
				stmt2.setInt(1,d.getId());
				ResultSet rs = stmt2.executeQuery();
				while(rs.next()) {
					boolean include=false;
					Datasource ds = new Datasource(this, rs.getInt(1), rs.getString(2));
					ds.setDomainId(rs.getInt(3));
					if (!m_OverrideAccessControl) {
						// Need to check permissions on this datasource. Only include it if it's both
						// viewable and readable
						char[] ar = accessRights.get(ds.getId());
						if (ar != null) {
							System.out.println("ar[0]="+ar[0]+" ar[1]="+ar[1]);
							boolean viewable=(ar[0]=='Y')?true:(ar[0]=='-')?d.isViewable(true):false; 
							boolean readable=(ar[1]=='Y')?true:(ar[1]=='-')?d.isReadable(true):false; 
							System.out.println("viewable="+viewable+" readable="+readable);
							if (viewable && readable) include=true;
						} else {
							// No override record
							System.out.println("no override record for "+ds.getId()+" ("+ds.getName()+"), taking domain permissions d.isViewable(true)="+d.isViewable(true)+" d.isReadable(true)="+d.isReadable(true));
							include = (d.isViewable(true) && d.isReadable(true));
						}
					} else {
						include=true;	// override access control
					}
					if (include) {
						ret.add(ds);
					} else {
						// debug
						System.out.println("Not adding action "+ds.getName());
					}
				}
				rs.close();
				d=d.getDomain();
			}
			stmt2.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve datasources from database");	
	}
	
	public List<Datasource> getLDAPDataSources(int domid)
	{
		String sql = "select a.id,a.name,a.domainid from dm.dm_datasource a,dm_providerdef b where a.defid=b.id and b.name='ldap' and a.domainid=?";
		return internalGetDataSource(sql,domid);	
	}
	
	public List<Datasource> getBugTrackerDataSources(int domid)
	{
		String sql = "select a.id,a.name,a.domainid from dm.dm_datasource a,dm_providerdef b where a.defid=b.id and b.name not in ('odbc','ldap') and a.domainid=?";
		return internalGetDataSource(sql,domid);
	}


	public PropertyDataSet getProviderDefDetails(ProviderDefinition def, Engine engine)
	{
		String sql = "SELECT p.name, p.value FROM dm.dm_providerdefprops p "
				   + "WHERE p.defid = ? AND p.engineid = ? ORDER BY 1";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, def.getId());
			stmt.setInt(2, engine.getId());
			ResultSet rs = stmt.executeQuery();
			PropertyDataSet ret = new PropertyDataSet();
			while(rs.next()) {
				ret.addProperty(rs.getString(1), rs.getString(2));
			}
			rs.close();
			stmt.close();
			return ret;
		} catch(Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve provider def props " + def.getId() + " from database");
	}

	
	public List<DMPropertyDef> getPropertyDefsForProviderDef(ProviderDefinition pd)
	{
		String sql = "SELECT p.name, p.required, p.appendable "
			+ "FROM dm.dm_propertydef p WHERE p.defid = ? ORDER BY 1";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, pd.getId());
			ResultSet rs = stmt.executeQuery();
			List<DMPropertyDef> ret = new ArrayList<DMPropertyDef>();
			while(rs.next()) {
				ret.add(new DMPropertyDef(rs.getString(1), getBoolean(rs, 2), getBoolean(rs, 3)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve property defs for provider def " + pd.getName() + " " + pd.getId() + " from database");				
	}
	
 public List<DMPropertyDef> getPropertyDefs(int defid)
 {
  String sql = "SELECT p.name, p.required, p.appendable "
   + "FROM dm.dm_propertydef p WHERE p.defid = ? ORDER BY 1";
  try
  {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, defid);
   ResultSet rs = stmt.executeQuery();
   List<DMPropertyDef> ret = new ArrayList<DMPropertyDef>();
   while(rs.next()) {
    ret.add(new DMPropertyDef(rs.getString(1), getBoolean(rs, 2), getBoolean(rs, 3)));
   }
   rs.close();
   stmt.close();
   return ret;
  }
  catch(SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  return null;
 }
	
	// Repository
	
	public Repository getRepository(int repid, boolean detailed)
	{
	 if (repid <= 0)
	 {
   Repository ret = new Repository(this, repid, "");
   ret.setName("");
   ret.setDef(new ProviderDefinition(this,0,""));
   ret.setSummary("");
   ret.setCredential(new Credential(this, 0, ""));
   return ret;
	 }
	 
		String sql = null;
		if(detailed) {
			sql = "SELECT r.name, r.summary, r.domainid, r.status, "
					+ "  r.credid, c.name, "
					+ "  uc.id, uc.name, uc.realname, r.created, "
					+ "  um.id, um.name, um.realname, r.modified, "
					+ "  uo.id, uo.name, uo.realname, g.id, g.name, c.domainid "
					+ "FROM dm.dm_repository r "
					+ "LEFT OUTER JOIN dm.dm_credentials c ON r.credid = c.id "		// credential
					+ "LEFT OUTER JOIN dm.dm_user uc ON r.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON r.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_user uo ON r.ownerid = uo.id "			// owner user
					+ "LEFT OUTER JOIN dm.dm_usergroup g ON r.ogrpid = g.id "		// owner group
					+ "WHERE r.id = ?";	
		} else {
			sql = "SELECT r.name, r.summary, r.domainid, r.status FROM dm.dm_repository r WHERE r.id = ?";	
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, repid);
			ResultSet rs = stmt.executeQuery();
			Repository ret = null;
			if(rs.next()) {
				ret = new Repository(this, repid, rs.getString(1),rs.getInt(3));
				ret.setSummary(rs.getString(2));
				ret.setDomainId(getInteger(rs, 3, 0));
				getStatus(rs, 4, ret);
				if(detailed) {
					int credid = getInteger(rs, 5, 0);
					if(credid != 0) {
						ret.setCredential(new Credential(this, credid, rs.getString(6),rs.getInt(20)));
					}
					getCreatorModifierOwner(rs, 7, ret);
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve repository " + repid + " from database");				
	}
	
 public List<Repository> getAccessibleRepositories(String objid,int domainid)
 {
	System.out.println("getAccessibleRepositories("+objid+","+domainid+")");
	if (objid.startsWith("task")) {
		objid="ta"+objid.substring(4);
	}
	ObjectTypeAndId x = new ObjectTypeAndId(objid);
	Domain d = null;
	if (x.getId()>0) {
		// Get domain from object
		DMObject obj = this.getObject(x.getObjectType(), x.getId());
		System.out.println("obj = "+obj.getName());
		d =  obj.getDomain();
	} else {
		// New object - get domain from tree
		d = getDomain(domainid);
	}
	Hashtable<Integer,char[]> accessRights = new Hashtable<Integer,char[]>();
	
	
	String sql1 = "select a.id,b.viewaccess,b.readaccess	"
			+	"from	dm.dm_repository		a,	"
			+	"		dm.dm_repositoryaccess	b,	"
			+	"		dm.dm_usersingroup		c	"
			+	"where	c.userid=?					"
			+	"and	c.groupid=b.usrgrpid		"
			+	"and	a.id=b.repositoryid			"
			+	"and	a.domainid=?				"
			+	"union	"
			+	"select	d.id,e.viewaccess,e.readaccess	"
			+	"from	dm.dm_repository		d,	"
			+	"		dm.dm_repositoryaccess	e	"
			+	"where	e.usrgrpid=1				"	// user group 1 = Everyone
			+	"and	d.id=e.repositoryid			"
			+	"and	d.domainid=?				";
	
	String sql2 = "SELECT r.id, r.name, r.summary, r.domainid, r.status "
			   + "FROM dm.dm_repository r "
			   + "WHERE r.domainid = ? AND r.status = 'N' ORDER BY 2";
	
	List<Repository> ret = new ArrayList<Repository>();
	try {
	PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
	PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
	while (d != null && d.getId()>=1) {
		System.out.println("domain is "+d.getName()+") read="+d.isReadable(true)+" write="+d.isWriteable(true)+" update="+d.isUpdatable(true)+" view="+d.isViewable(true));
		if (!m_OverrideAccessControl) {
			// Get a list of actions in this domain with non-default access permissions
			System.out.println("no override access, getting list of overrides userid="+getUserID()+" domainid="+d.getId());
			stmt1.setInt(1,getUserID());
			stmt1.setInt(2,d.getId());
			stmt1.setInt(3,d.getId());
			ResultSet rs = stmt1.executeQuery();
			while (rs.next()) {
				char[] ar = new char[2];
				ar[0] = getString(rs,2,"-").charAt(0);
				ar[1] = getString(rs,3,"-").charAt(0);
				System.out.println("id="+rs.getInt(1)+" ar[0]="+ar[0]+" ar[1]="+ar[1]);
				accessRights.put(rs.getInt(1),ar);
			}
			rs.close();
		}
		stmt2.setInt(1,d.getId());
		ResultSet rs = stmt2.executeQuery();
		while(rs.next()) {
			boolean include=false;
			Repository rep = new Repository(this, rs.getInt(1), rs.getString(2));
			if (!m_OverrideAccessControl) {
				// Need to check permissions on this action. Only include it if it's both
				// viewable and readable
				char[] ar = accessRights.get(rep.getId());
				if (ar != null) {
					System.out.println("ar[0]="+ar[0]+" ar[1]="+ar[1]);
					boolean viewable=(ar[0]=='Y')?true:(ar[0]=='-')?d.isViewable(true):false; 
					boolean readable=(ar[1]=='Y')?true:(ar[1]=='-')?d.isReadable(true):false; 
					System.out.println("viewable="+viewable+" readable="+readable);
					if (viewable && readable) include=true;
				} else {
					// No override record
					System.out.println("no override record for "+rep.getId()+" ("+rep.getName()+"), taking domain permissions d.isViewable(true)="+d.isViewable(true)+" d.isReadable(true)="+d.isReadable(true));
					include = (d.isViewable(true) && d.isReadable(true));
				}
			} else {
				include=true;	// override access control
			}
			if (include) {
				rep.setDomainId(rs.getInt(4));
				rep.setSummary(rs.getString(3));
				ret.add(rep);
			} else {
				// debug
				System.out.println("Not adding repository "+rep.getName());
			}
		}
		rs.close();
		d=d.getDomain();
	}
	stmt2.close();
	return ret;
 }
 catch(SQLException ex)
 {
	ex.printStackTrace();
	rollback();
 }

 throw new RuntimeException("Unable to retrieve accessible repositories from database");    
 }
 
	
	public List<Repository.TextPattern> getRepositoryTextPatterns(Repository repo)
	{
		String sql = "SELECT p.path, p.pattern, p.istext "
			+ "FROM dm.dm_repositorytextpattern p "
			+ "WHERE p.repositoryid = ? ORDER BY 1, 2";	
	
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, repo.getId());
			ResultSet rs = stmt.executeQuery();
			List<Repository.TextPattern> ret = new ArrayList<Repository.TextPattern>();
			while(rs.next()) {
				Repository.TextPattern pattern = repo.new TextPattern(rs.getString(1), rs.getString(2), getBoolean(rs, 3));
				ret.add(pattern);
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve accessible repositories from database");				
	}
	
	
	public boolean updateRepositoryTextPatterns(Repository repo, ACDChangeSet<Repository.TextPattern> changes)
	{
		System.out.println("updateRepositoryTextPatterns");
		String dsql = "DELETE FROM dm.dm_repositorytextpattern p WHERE p.repositoryid = ? AND p.path = ? AND p.pattern = ?";
		String asql = "INSERT INTO dm.dm_repositorytextpattern(repositoryid,path,pattern,istext) VALUES(?,?,?,?)";
		String csql = "UPDATE dm.dm_repositorytextpattern p SET istext = ?, path = ?, pattern = ? WHERE p.repositoryid = ? AND p.path = ?";
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(dsql);
			for(Repository.TextPattern p : changes.deleted()) {
				System.out.println("Deleting " + repo.getId() + " '" + p.getPath() + "' '" + p.getPattern() + "' from repositorytextpattern");
				stmt.setInt(1, repo.getId());
				stmt.setString(2, p.getPath());
				stmt.setString(3, p.getPattern());
				stmt.execute();
			}
			stmt.close();
			
			stmt = getDBConnection().prepareStatement(asql);
			for(Repository.TextPattern p : changes.added()) {
				System.out.println("Inserting " + repo.getId() + " '" + p.getPath() + "' '" + p.getPattern() + "' into repositorytextpattern");
				stmt.setInt(1, repo.getId());
				stmt.setString(2, p.getPath());
				stmt.setString(3, p.getPattern());
				stmt.setString(4, p.isText() ? "Y" : "N");
				stmt.execute();
			}
			stmt.close();

			stmt = getDBConnection().prepareStatement(csql);
			for(Repository.TextPattern p : changes.changed()) {
				System.out.println("Updating " + repo.getId() + " '" + p.getPath() + "' '" + p.getPattern() + "' in repositorytextpattern");
				stmt.setString(1, p.isText() ? "Y" : "N");
    stmt.setString(2, p.getPath());
    stmt.setString(3, p.getPattern());
				stmt.setInt(4, repo.getId());
				stmt.setString(5, p.getKey());
				stmt.execute();
			}
			stmt.close();

			getDBConnection().commit();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	// Datasource
	
	public Datasource getDatasource(int dsid, boolean detailed)
	{
	 if (dsid < 0)
	 {
	  Datasource ret = new Datasource(this, dsid, "");
	  ret.setName("");
	  ret.setDef(new ProviderDefinition(this,0,""));
   ret.setSummary("");
   return ret;
	 }
	 
		String sql = null;
		if(detailed) {
			sql = "SELECT d.name, d.summary, d.domainid, d.status, d.credid, c.name, c.domainid, "
				+ "  uc.id, uc.name, uc.realname, d.created, "
				+ "  um.id, um.name, um.realname, d.modified, "
				+ "  uo.id, uo.name, uo.realname, g.id, g.name "
				+ "FROM dm.dm_datasource d "
				+ "LEFT OUTER JOIN dm.dm_credentials c ON d.credid = c.id "		// credential
				+ "LEFT OUTER JOIN dm.dm_user uc ON d.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON d.modifierid = um.id "		// modifier
				+ "LEFT OUTER JOIN dm.dm_user uo ON d.ownerid = uo.id "			// owner user
				+ "LEFT OUTER JOIN dm.dm_usergroup g ON d.ogrpid = g.id "		// owner group
				+ "WHERE d.id = ?";	
		} else {
			sql = "SELECT d.name, d.summary, d.domainid, d.status, d.credid, c.name, c.domainid "
				+ "FROM dm.dm_datasource d "
				+ "LEFT OUTER JOIN dm.dm_credentials c ON d.credid = c.id "		// credential
				+" WHERE d.id = ?";	
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, dsid);
			ResultSet rs = stmt.executeQuery();
			Datasource ret = null;
			if (rs.next()) {
				ret = new Datasource(this, dsid, rs.getString(1));
				ret.setSummary(rs.getString(2));
				ret.setDomainId(getInteger(rs, 3, 0));
				getStatus(rs, 4, ret);
				int credid = getInteger(rs, 5, 0);
				if (credid != 0) {
					ret.setCredential(new Credential(this, credid, rs.getString(6),rs.getInt(7)));
				}
				if (detailed) {
					getCreatorModifierOwner(rs, 8, ret);
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve datasource " + dsid + " from database");				
	}
	
	// Notify
	
	public Notify getNotify(int nfyid, boolean detailed)
	{
		if (nfyid < 0)
		{
			 Notify ret = new Notify(this, nfyid, "");
			 ret.setName("");
			 ret.setSummary("");
			 ret.setCredential(new Credential(this, 0, ""));
			 ret.setDef(new ProviderDefinition(this,0,""));
			 return ret;
		}
	 
		String sql = null;
		if(detailed) {
			sql = "SELECT n.name, n.summary, n.domainid, n.status, n.credid, c.name, c.domainid, "
				+ "  uc.id, uc.name, uc.realname, n.created, "
				+ "  um.id, um.name, um.realname, n.modified, "
				+ "  uo.id, uo.name, uo.realname, g.id, g.name "
				+ "FROM dm.dm_notify n "
				+ "LEFT OUTER JOIN dm.dm_credentials c ON n.credid = c.id "		// credential
				+ "LEFT OUTER JOIN dm.dm_user uc ON n.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON n.modifierid = um.id "		// modifier
				+ "LEFT OUTER JOIN dm.dm_user uo ON n.ownerid = uo.id "			// owner user
				+ "LEFT OUTER JOIN dm.dm_usergroup g ON n.ogrpid = g.id "		// owner group
				+ "WHERE n.id = ?";	
		} else {
			sql = "SELECT n.name, n.summary, n.domainid, n.status, n.credid, c.name, c.domainid "
				+ "FROM dm.dm_notify n "
				+ "LEFT OUTER JOIN dm.dm_credentials c ON n.credid = c.id "		// credential
				+ "WHERE n.id = ?";	
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, nfyid);
			ResultSet rs = stmt.executeQuery();
			Notify ret = null;
			if(rs.next()) {
				ret = new Notify(this, nfyid, rs.getString(1));
				ret.setSummary(rs.getString(2));
				ret.setDomainId(getInteger(rs, 3, 0));
				getStatus(rs, 4, ret);
				int credid = getInteger(rs, 5, 0);
				if (credid != 0) {
					ret.setCredential(new Credential(this, credid, rs.getString(6),rs.getInt(7)));
				}
				if (detailed) {
					getCreatorModifierOwner(rs, 8, ret);
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve notify " + nfyid + " from database");				
	}
	
	// Notify
	
	public NotifyTemplate getTemplate(int templateid)
	{
		if (templateid < 0) {
			NotifyTemplate ret = new NotifyTemplate(this, templateid, "");
			ret.setName("");
			ret.setSummary("");
			return ret;
		}
		String sql = "SELECT n.name, n.notifierid, n.summary, n.status, n.subject, n.body, "
				+ "  uc.id, uc.name, uc.realname, n.created, "
				+ "  um.id, um.name, um.realname, n.modified "
				+ "FROM dm.dm_template n "
				+ "LEFT OUTER JOIN dm.dm_user uc ON n.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON n.modifierid = um.id "		// modifier
				+ "WHERE n.id = ?";	
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, templateid);
			ResultSet rs = stmt.executeQuery();
			NotifyTemplate ret = null;
			if(rs.next()) {
				int notifierid = rs.getInt(2);
				if (!rs.wasNull() && notifierid>0) {
					// Get the domain from the linked notifier
					String sql2 = "SELECT domainid FROM dm.dm_notify WHERE id=?";
					PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
					stmt2.setInt(1,notifierid);
					ResultSet rs2 = stmt2.executeQuery();
					if (rs2.next()) {
						ret = new NotifyTemplate(this, templateid, rs.getString(1),rs2.getInt(1));
						ret.setNotifierId(notifierid);
					} else {
						ret = new NotifyTemplate(this, templateid, rs.getString(1));
					}
					rs2.close();
				} else {
					ret = new NotifyTemplate(this, templateid, rs.getString(1));
				}
				ret.setSummary(rs.getString(3));
				getStatus(rs, 4, ret);
				ret.setSubject(rs.getString(5));
				ret.setBody(rs.getString(6));
				getCreatorModifier(rs, 7, ret);
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve template " + templateid + " from database");				
	}

	public List<NotifyTemplate> getTemplates(Notify notify)
	{
		String sql = "SELECT n.id, n.name, n.summary, n.status, "
				+ "  uc.id, uc.name, uc.realname, n.created, "
				+ "  um.id, um.name, um.realname, n.modified "
				+ "FROM dm.dm_template n "
				+ "LEFT OUTER JOIN dm.dm_user uc ON n.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON n.modifierid = um.id "		// modifier
				+ "WHERE n.notifierid = ?";	
		List <NotifyTemplate> res = new ArrayList<NotifyTemplate>();;
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, notify.getId());
			ResultSet rs = stmt.executeQuery();
			NotifyTemplate ret = null;
			while (rs.next()) {
				ret = new NotifyTemplate(this, rs.getInt(1), rs.getString(2));
				ret.setSummary(rs.getString(3));
				getStatus(rs, 4, ret);
				getCreatorModifier(rs, 5, ret);
				res.add(ret);
			}
			rs.close();
			stmt.close();
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve templates for notification process " + notify.getId());				
	}
	
	public List<BuildJob> getBuildJobs(Builder builder)
	{
		String sql = "SELECT b.id, b.name, b.summary, b.status, "
				+ "  uc.id, uc.name, uc.realname, b.created, "
				+ "  um.id, um.name, um.realname, b.modified "
				+ "FROM dm.dm_buildjob b "
				+ "LEFT OUTER JOIN dm.dm_user uc ON b.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON b.modifierid = um.id "		// modifier
				+ "WHERE b.builderid = ?";	
		List <BuildJob> res = new ArrayList<BuildJob>();;
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, builder.getId());
			ResultSet rs = stmt.executeQuery();
			BuildJob ret = null;
			while (rs.next()) {
				ret = new BuildJob(this, rs.getInt(1), rs.getString(2));
				ret.setSummary(rs.getString(3));
				getStatus(rs, 4, ret);
				getCreatorModifier(rs, 5, ret);
				res.add(ret);
			}
			rs.close();
			stmt.close();
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve build jobs for buildengine " + builder.getId());				
	}
	
	
	
	// Credential
	
	public Credential getCredential(int credid, boolean detailed)
	{
		if (credid < 0) {
			Credential ret = new Credential(this, credid, "");
			ret.setName("");
			ret.setSummary("");
			CredentialKind kind = CredentialKind.UNCONFIGURED;
			ret.setKind(kind);
			ret.setVarUsername("");
			ret.setVarPassword("");
			return ret; 
	 	}
	 
		String sql = null;
		if(detailed) {
			sql = "SELECT c.name, c.summary, c.domainid, c.kind, c.status, c.ownerid, c.ogrpid, " 
				+ "  c.encusername, c.encpassword, c.filename, "
				+ "  uc.id, uc.name, uc.realname, c.created, "
				+ "  um.id, um.name, um.realname, c.modified "
				+ "FROM dm.dm_credentials c "
				+ "LEFT OUTER JOIN dm.dm_user uc ON c.creatorid = uc.id "		// creator
				+ "LEFT OUTER JOIN dm.dm_user um ON c.modifierid = um.id "		// modifier
				+ "WHERE c.id = ?";	
		} else {
			sql = "SELECT c.name, c.summary, c.domainid, c.kind, c.status, c.ownerid, c.ogrpid FROM dm.dm_credentials c WHERE c.id = ?";
		}
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, credid);
			ResultSet rs = stmt.executeQuery();
			Credential ret = null;
			if(rs.next()) {
				ret = new Credential(this, credid, rs.getString(1), rs.getInt(3));
				ret.setSummary(rs.getString(2));
				ret.setDomainId(getInteger(rs, 3, 0));
				CredentialKind kind = CredentialKind.fromInt(rs.getInt(4)); 
				ret.setKind(kind);
				getStatus(rs, 5, ret);
				int owner = rs.getInt(6);
				boolean credowner=false;
				if (rs.wasNull()) {
					System.out.println("user was null - setting group");
					UserGroup grp = getGroup(getInteger(rs,7,0));
					if (grp != null) {
						ret.setOwner(grp);
						UserList users = getUsersInGroup(grp.getId());
						for (User user: users) {
							if (user.getId()==getUserID()) {
								credowner=true;
								break;
							}
						}
					}
				} else {
					// Owner must be a user
					System.out.println("Owner is a user m_userID="+getUserID()+" owner="+owner);
					credowner = (getUserID() == owner);
					System.out.println("about to getUser");
					User user = getUser(owner);
					System.out.println("Returned from getUser");
					ret.setOwner(user);
				}
				System.out.println("credowner="+credowner+" kind="+kind);
				if(detailed) {
					switch(kind) {
					case IN_DATABASE:
						System.out.println("username is in database");
						if (credowner) {
							System.out.println("we own credential - decrypt the username");
							String un = getString(rs,8,"");
							System.out.println("un="+un);
							if (un.length()>0) {
								byte[] dun = Decrypt3DES(un,m_passphrase);
								String dstr = new String(dun);
								System.out.println("dstr=["+dstr+"]");
								ret.setPlainUsername(dstr);
							}
						}
						break;
					case FROM_VARS:
						System.out.println("from vars");
						ret.setVarUsername(getString(rs, 8, ""));
						ret.setVarPassword(getString(rs, 9, ""));
						break;
					case PPK:
						ret.setVarUsername(getString(rs, 8, ""));
						// deliberate drop-through...
					case RTI3_DFO_IN_FILESYSTEM:
					case HARVEST_DFO_IN_FILESYSTEM:
						ret.setFilename(getString(rs, 10, ""));
						default: break;
					}
					getCreatorModifier(rs, 11, ret);
				}
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve credential " + credid + " from database");				
	}
	
	
	public boolean updateCredential(Credential cred, SummaryChangeSet changes)
	{
		System.out.println("updateCredential");
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_credentials ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		int dup = 0;
  for(SummaryField field : changes) {
   switch(field) {
    case CRED_ENCUSERNAME:
    case CRED_VARUSERNAME:
    case CRED_USERNAME: {
      dup++;
     }
     break;
    default:
     break;
   }
  }
   
		for(SummaryField field : changes) {
			switch(field) {
			case CRED_KIND: {
				System.out.println("CRED_KIND");
				if(cred.getKind() != CredentialKind.UNCONFIGURED) {
					throw new RuntimeException("Credential kind cannot be changed after creation");
				}
				CredentialKind kind = (CredentialKind) changes.get(field);
				if(kind == null) {
					throw new RuntimeException("Credential kind must be specified");					
				}
				update.add(", kind = ?", kind.value());
				}
				break;
			case CRED_ENCUSERNAME:
			case CRED_VARUSERNAME:
			case CRED_USERNAME: {
			  if (dup <= 1 || (dup > 1 && field == SummaryField.CRED_ENCUSERNAME))  // fix dup encusername being passed to query
			  { 
				  System.out.println("CRED_ENCUSERNAME/CRED_USERNAME/CRED_VARUSERNAME");
				  String username = (String) changes.get(field);
				  System.out.println("username="+username);
				  update.add(", encusername = ?", (username != null) ? username : Null.STRING);
				 }
			 }
				break;
			case CRED_ENCPASSWORD:
			case CRED_VARPASSWORD: {
				System.out.println("CRED_ENCPASSWORD/CRED_VARPASSWORD");
				String passwd = (String) changes.get(field);
				System.out.println("password="+passwd);
				update.add(", encpassword = ?", (passwd != null) ? passwd : Null.STRING);
				}
				break;
			case CRED_FILENAME: {
				System.out.println("CREDFILENAME");
				String filename = (String) changes.get(field);
				update.add(", filename = ?", (filename != null) ? filename : Null.STRING);
				}
				break;
			default:
				System.out.println("default: field.value="+field.value());
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(cred,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", cred.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	public List<Credential> getCredentials()
	{
		String sql = "SELECT c.id, c.name, c.domainid FROM dm.dm_credentials c "
			+ "WHERE c.status = 'N' AND c.kind <> 0 AND c.domainid IN (" + m_domainlist + ") ORDER BY 2";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			List<Credential> ret = new ArrayList<Credential>();
			while(rs.next()) {
				ret.add(new Credential(this, rs.getInt(1), rs.getString(2), rs.getInt(3)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to retrieve credentials from database");				
	}
	
	
	
	// Transfer
	
	public List<Transfer> getTransfers()
	{
		String sql = "SELECT p.id, p.name FROM dm.dm_providerdef p WHERE p.kind = " + ObjectType.TRANSFER.value() + " ORDER BY 2";
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			List<Transfer> ret = new ArrayList<Transfer>();
			while(rs.next()) {
				ret.add(new Transfer(this, rs.getInt(1), rs.getString(2)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve plugins for transfers from database");						
	}
	
	
	// Engine
	
	public Engine getEngine(int engineid)
	{
		String sql = "SELECT e.name, e.hostname, e.status, "
			+ "  uc.id, uc.name, uc.realname, e.created, "
			+ "  um.id, um.name, um.realname, e.modified "
			+ "FROM dm.dm_engine e "
			+ "LEFT OUTER JOIN dm.dm_user uc ON e.creatorid = uc.id "		// creator
			+ "LEFT OUTER JOIN dm.dm_user um ON e.modifierid = um.id "		// modifier
			+ "WHERE e.id = ?";	
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, engineid);
			ResultSet rs = stmt.executeQuery();
			Engine ret = null;
			if(rs.next()) {
				ret = new Engine(this, engineid, rs.getString(1));
				ret.setHostname(rs.getString(2));
				getStatus(rs, 3, ret);
				getCreatorModifier(rs, 4, ret);
			}
			rs.close();
			stmt.close();
			if(ret != null) {
				return ret;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve engine " + engineid + " from database");				
	}
	
 public Engine getEngine4Domain(int domainid)
 {
  String sql = "SELECT e.id, e.name, e.hostname, e.status, "
   + "  uc.id, uc.name, uc.realname, e.created, "
   + "  um.id, um.name, um.realname, e.modified "
   + "FROM dm.dm_engine e "
   + "LEFT OUTER JOIN dm.dm_user uc ON e.creatorid = uc.id "  // creator
   + "LEFT OUTER JOIN dm.dm_user um ON e.modifierid = um.id "  // modifier
   + "WHERE e.domainid = ?"; 
  
  try
  {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   stmt.setInt(1, domainid);
   ResultSet rs = stmt.executeQuery();
   Engine ret = null;
   if(rs.next()) {
    ret = new Engine(this, rs.getInt(1), rs.getString(2));
    ret.setHostname(rs.getString(3));
    getStatus(rs, 4, ret);
    getCreatorModifier(rs, 5, ret);
   }
   rs.close();
   stmt.close();
    return ret;
  }
  catch(SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  throw new RuntimeException("Unable to retrieve engine " + domainid + " from database");    
 }

	
	public boolean updateEngine(Engine eng, SummaryChangeSet changes)
	{
		DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_engine ");
		update.add("SET modified = ?, modifierid = ?", timeNow(), getUserID());
				
		for(SummaryField field : changes) {
			switch(field) {
			case ENGINE_HOSTNAME:
				update.add(", hostname = ?", changes.get(field));
				update.add(", name = ?", changes.get(field));
				break;
			default:
				if(field.value() <= SummaryField.OBJECT_MAX) {
					updateObjectSummaryField(eng,update, field, changes);
				} else {
					System.err.println("ERROR: Unhandled summary field " + field);
				}
				break;
			}
		}
		
		update.add(" WHERE id = ?", eng.getId());
		
		try {
			update.execute();
			getDBConnection().commit();
			update.close();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	
	public List<Plugin> getPluginsForEngine(Engine engine)
	{
		String sql = "SELECT p.id, p.library, p.version FROM dm.dm_plugin p ORDER BY 2"; //WHERE p.engineid = ?";	
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			//stmt.setInt(1, engine.getId());
			ResultSet rs = stmt.executeQuery();
			List<Plugin> ret = new ArrayList<Plugin>();
			while(rs.next()) {
				Plugin p = new Plugin(this, rs.getInt(1), rs.getString(2));
				p.setVersion(rs.getString(3));
				ret.add(p);
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve plugins for engine " + engine.getId() + " from database");				
	}
	
	
	public List<Engine.ConfigEntry> getConfigForEngine(Engine engine)
	{
		String sql = "SELECT c.name, c.value FROM dm.dm_engineconfig c WHERE c.engineid = ? ORDER BY 1";	
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1, engine.getId());
			ResultSet rs = stmt.executeQuery();
			List<Engine.ConfigEntry> ret = new ArrayList<Engine.ConfigEntry>();
			while(rs.next()) {
				ret.add(engine.new ConfigEntry(rs.getString(1), rs.getString(2)));
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve plugins for engine " + engine.getId() + " from database");				
	}
	
	
	public List<ProviderDefinition> getProviderDefsForEngine(Engine engine)
	{
		String sql = "SELECT pd.id, pd.name, pd.kind FROM dm.dm_providerdef pd WHERE NOT pd.id = 0 ORDER BY 2"; //WHERE pd.engineid = ?";	
		
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			//stmt.setInt(1, engine.getId());
			ResultSet rs = stmt.executeQuery();
			List<ProviderDefinition> ret = new ArrayList<ProviderDefinition>();
			while(rs.next()) {
				ProviderDefinition pd = new ProviderDefinition(this, rs.getInt(1), rs.getString(2));
				pd.setKind(ObjectType.fromInt(rs.getInt(3)));
				ret.add(pd);
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve provider definitions for engine " + engine.getId() + " from database");				
	}

	
	private static boolean getBoolean(ResultSet rs, int col)
	{
		String str;
		try {
			str = rs.getString(col);
			return (str != null) ? str.equalsIgnoreCase("Y") : false;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public DMActionEditInfo GetActionEditInfo(int actionid)
	{
		try
		{
			Statement st1 = getDBConnection().createStatement();
			ResultSet rs1 = st1.executeQuery("SELECT editid,userid FROM dm.dm_actionedit WHERE actionid="+actionid);
			if (rs1.next())
			{
				DMActionEditInfo aei = new DMActionEditInfo();
				aei.setEditID(rs1.getInt(1));
				aei.setUserID(rs1.getInt(2));
				return aei;
			}
			else
			{
				return null;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action edit info from database");				
	}
	
	
	public List <DMActionNode> GetActionNodes(int actionid,int windowid)
	{
		List <DMActionNode> ret = new ArrayList<DMActionNode>();
		String q1 = "SELECT a.windowid,a.xpos,a.ypos,a.typeid,a.title,a.summary,b.name,b.exitpoints,b.drilldown,b.actionid,b.functionid FROM dm.dm_actionfrags a,dm.dm_fragments b WHERE a.actionid="+actionid+" AND b.id=a.typeid AND a.windowid>0 AND a.parentwindowid="+windowid+" ORDER BY a.windowid";
		String q2 = "SELECT a.windowid,a.xpos,a.ypos,a.typeid,a.title,a.summary,b.name,b.exitpoints,b.drilldown,b.actionid,b.functionid FROM dm.dm_actionfrags a,dm.dm_fragments b WHERE a.actionid="+actionid+" AND b.id=a.typeid AND a.windowid>0 AND a.parentwindowid IS NULL ORDER BY a.windowid";
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery(windowid>0?q1:q2);
			while (rs.next())
			{
				DMActionNode an = new DMActionNode();
				an.setNodeID(rs.getInt(1));
				an.setDescriptor(rs.getString(7));
				an.setXpos(rs.getInt(2));
				an.setYpos(rs.getInt(3));
				int typeid = rs.getInt(4);
				an.setTypeID(typeid);
				an.setTitle(rs.getString(5));
				if (rs.wasNull()) an.setTitle("");
				an.setSummary(rs.getString(6));
				if (rs.wasNull()) an.setSummary("");
				an.setExitPoints(rs.getInt(8));
				an.setDrillDown(rs.getString(9));
				int procid = getInteger(rs,10,0);
				int funcid = getInteger(rs,11,0);
				an.setProcedureID(procid);
				an.setFunctionID(funcid);
				if (procid>0 || funcid>0) {
					PreparedStatement kindstmt=getDBConnection().prepareStatement("SELECT kind FROM dm.dm_action WHERE id=?");
					kindstmt.setInt(1,procid>0?procid:funcid);
					ResultSet kindrs=kindstmt.executeQuery();
					if (kindrs.next()) {
						an.setKind(kindrs.getInt(1));
					}
					kindrs.close();
					kindstmt.close();
				}
				ret.add(an);
			}
			rs.close();
			st.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action nodes from database");	
	}
	
	public List <DMActionLink> GetActionLinks(int actionid,int windowid)
	{
		List <DMActionLink> ret = new ArrayList<DMActionLink>();
		String q1="SELECT distinct a.flowid,a.nodefrom,a.nodeto,a.pos FROM dm.dm_actionflows a,dm.dm_actionfrags b where a.nodeto=b.windowid AND b.parentwindowid is null and b.actionid=a.actionid and a.actionid="+actionid;
		String q2="SELECT distinct a.flowid,a.nodefrom,a.nodeto,a.pos FROM dm.dm_actionflows a,dm.dm_actionfrags b where a.nodeto=b.windowid AND b.parentwindowid="+windowid+" and b.actionid=a.actionid and a.actionid="+actionid;
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery(windowid>0?q2:q1);
			while (rs.next())
			{
				DMActionLink an = new DMActionLink();
				an.setFlowID(rs.getInt(1));
				an.setNodeFrom(rs.getInt(2));
				an.setNodeTo(rs.getInt(3));
				an.setPos(rs.getInt(4));
				ret.add(an);
			}
			rs.close();
			st.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve action links from database");	
	}
	
	public int getStartXPosition(int actionid,int pw)
	{
		String q1="SELECT xpos FROM dm.dm_actionfrags WHERE actionid="+actionid+" AND windowid=0 AND parentwindowid IS NULL";
		String q2="SELECT xpos FROM dm.dm_actionfrags WHERE actionid="+actionid+" AND windowid=0 AND parentwindowid="+pw;
		try
		{
			int ret=0;	// client side to use default position
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery(pw>0?q2:q1);
			if (rs.next())
			{
				ret = rs.getInt(1);
			}
			rs.close();
			st.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to start window position from database");	
	}
		
	public FragmentDetails getFragmentDetails(int actionid,int windowid)
	{
		try
		{
			String sql = 	"SELECT a.name,a.summary,a.exitpoints,a.drilldown,b.title,b.summary,b.parentwindowid,a.actionid	"
						+	"FROM dm.dm_fragments a,dm.dm_actionfrags b 				"
						+	"where a.id = b.typeid and b.actionid=? and b.windowid=?	";
			
			FragmentDetails res = new FragmentDetails();
			
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,actionid);
			stmt.setInt(2,windowid);
			ResultSet rs = stmt.executeQuery();
			
			if (rs.next())
			{
				res.setTypeName(rs.getString(1));
				res.setTypeSummary(rs.getString(2));
				res.setExitPoints(rs.getInt(3));
				res.setDrilldown(getString(rs, 4, "N").equalsIgnoreCase("Y"));
				res.setFragmentName(getString(rs, 5, ""));
				res.setFragmentSummary(getString(rs, 6, ""));
				res.setParentWindow(getInteger(rs,7,0));
				res.setFragmentAction(getInteger(rs,8,0));
				res.setActionId(actionid);
				res.setWindowId(windowid);
			}
			rs.close();
			stmt.close();
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to get Fragment Details from database");
	}
	
	private String getDomainHeirarchy(int domainid)
	{
		System.out.println("getDomainHeirarchy("+domainid+")");
		// Returns a list of domains which are parents of the specified domain
		String res="";
		String sep="";
		Domain domain = getDomain(domainid);
		while (domain != null) {
			res+=sep+String.valueOf(domain.getId());
			sep=",";
			domain = domain.getDomain();
		}
		System.out.println("getDomainHeirarchy returns "+res);
		return res;
	}
	
	/*
	private String getFQDN(int domainid)
	{
		System.out.println("getFQDN("+domainid+")");
		// Returns a list of domains which are parents of the specified domain
		String res="";
		String sep="";
		Domain domain = getDomain(domainid);
		while (domain != null) {
			res=domain.getName()+sep+res;
			if (domain.getId()==m_userDomain) break;
			sep=".";
			domain = domain.getDomain();
		}
		System.out.println("getFQDN returns "+res);
		return res;
	}
	*/
	
	public List<FragmentAttributes> getFragmentAttributes(int actionid,int windowid,int pwid)
	{
		System.out.println("getFragmentAttes,actionid="+actionid);
		Action action = getAction(actionid,true);
		System.out.println("Domain is "+action.getDomainId());
		List <FragmentAttributes> res = new ArrayList<FragmentAttributes>();;
		try
		{
//   String sql = "select a.id,a.attype,a.atname,a.tablename,a.inherit,c.value, d.inpos from dm.dm_fragmentattrs a left outer join dm.dm_actionfrags b on a.typeid=b.typeid left outer join dm.dm_actionfragattrs c on c.windowid=b.windowid and c.actionid=b.actionid and c.attrid=a.id left outer join dm.dm_actionarg d on a.atname = d.name where b.actionid=? and b.windowid=? order by d.inpos";
   String sql = "select a.id,a.attype,a.atname,a.tablename,a.inherit,c.value,a.required, a.default_value "
     + "from dm.dm_fragmentattrs a "
     + "left outer join dm.dm_actionfrags b on a.typeid=b.typeid "
     + "left outer join dm.dm_actionfragattrs c on c.windowid=b.windowid and c.actionid=b.actionid and c.attrid=a.id "
     + "where b.actionid=? "
     + "and b.windowid=? "
     + ((pwid>0)?"and b.parentwindowid=? ":"and b.parentwindowid is null ")
     + "order by a.atorder";

		 PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,actionid);
			stmt.setInt(2,windowid);
			if (pwid>0) stmt.setInt(3,pwid);
			ResultSet rs = stmt.executeQuery();

			while (rs.next())
			{
				FragmentAttributes fa = new FragmentAttributes();
				fa.setAttrId(rs.getInt(1));
				fa.setAttrType(rs.getString(2));
				fa.setAttrName(rs.getString(3));
				fa.setTableName(rs.getString(4));
				String inherit = rs.getString(5);
				fa.setInherit(rs.wasNull()?false:inherit.equalsIgnoreCase("Y"));
				fa.setAttrVal(getString(rs,6,""));
				String required = rs.getString(7);
				fa.setRequired(rs.wasNull()?false:required.equalsIgnoreCase("Y"));
				fa.setDefaultValue(rs.getString(8));
				if (fa.getAttrType().equalsIgnoreCase("dropdown"))
				{
					// Add the value list from the appropriate table
					List<FragmentListValues> flvs = new ArrayList<FragmentListValues>();
					String tabname = fa.getTableName();
					if (tabname.equalsIgnoreCase("dm_component") || tabname.equalsIgnoreCase("dm_server")
						|| tabname.equalsIgnoreCase("dm_application")) {
						// add var name as an option on the drop down
						FragmentListValues f = new FragmentListValues();
						f.setId(0);
						String vname = "${"+tabname.substring(3)+".name}";
						f.setName(vname);
						f.setSelected(vname.compareToIgnoreCase(fa.getAttrVal())==0);
						flvs.add(f);
					}
					
					String domainlist = getDomainHeirarchy(action.getDomainId());
					Statement st2 = getDBConnection().createStatement();
					ResultSet rs2 = st2.executeQuery(
						fa.getInherit()?
							"SELECT id,name,domainid FROM dm."+tabname+" WHERE domainid IN ("+domainlist+")":
							"SELECT id,name,domainid FROM dm."+tabname+" WHERE domainid="+action.getDomainId()
					);
					while (rs2.next())
					{
						FragmentListValues flv = new FragmentListValues();
						boolean selected=false;
						int domainid = rs2.getInt(3);
						if (ValidDomain(domainid,fa.getInherit()))
						{
							String name;
							if (tabname.equalsIgnoreCase("dm_task")) {
								name = getDomain(domainid).getFullDomain()+"."+rs2.getString(2);
								// name = getFQDN(domainid)+"."+rs2.getString(2);
							} else {
								name = rs2.getString(2);
							}
							if (name.compareToIgnoreCase(fa.getAttrVal())==0) selected=true;
							flv.setName(name);	
							flv.setId(rs2.getInt(1));
							flv.setSelected(selected);
							flvs.add(flv);
						}
					}
					rs2.close();
					st2.close();
					fa.setFragmentListValues(flvs);
				}
				res.add(fa);
			}
			rs.close();
			stmt.close();
			return res;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to get fragment attributes from database");
	}
	
	public ExplorerTabsList getExplorerTabs(int id)
	{
		System.out.println("in getExplorerTabs("+id+")");
		ExplorerTabsList ret = new ExplorerTabsList();
		switch (id) {
		case HOME_TAB_WORKBENCH: 
			ret.add(new ExplorerTabs(EXPLORER_TAB_WORKBENCH_WORKFLOW, id, "Applications", "workflow", "N"));
			ret.add(new ExplorerTabs(EXPLORER_TAB_WORKBENCH_ENVIRONMENTS, id, "Environments", "environments", "N"));
			break;
		case HOME_TAB_ENDPOINTS_AND_CREDENTIALS:
			ret.add(new ExplorerTabs(EXPLORER_TAB_ENDPOINTS_AND_CREDENTIALS_ENDPOINTS , id, "EndPoints", "endpoints", "Y"));
			ret.add(new ExplorerTabs(EXPLORER_TAB_ENDPOINTS_AND_CREDENTIALS_SERVERS , id, "Servers", "servers", "Y"));
			ret.add(new ExplorerTabs(EXPLORER_TAB_ENDPOINTS_AND_CREDENTIALS_CREDENTIALS , id, "Credentials", "credentials", "Y"));
			break;
		case HOME_TAB_APPLICATIONS_AND_COMPONENTS:
			ret.add(new ExplorerTabs(EXPLORER_TAB_APPLICATIONS_AND_COMPONENTS_APPLICATIONS, id, "Applications", "applications", "Y"));
			ret.add(new ExplorerTabs(EXPLORER_TAB_APPLICATIONS_AND_COMPONENTS_COMPONENTS, id, "Components", "components", "Y"));
			break;
		case HOME_TAB_ACTIONS_AND_PROCEDURES:
			ret.add(new ExplorerTabs(EXPLORER_TAB_ACTIONS_AND_PROCEDURES_ACTIONS, id, "Actions", "actions", "Y"));
			ret.add(new ExplorerTabs(EXPLORER_TAB_ACTIONS_AND_PROCEDURES_PROCEDURES, id, "Procedures", "procedures", "Y"));
			ret.add(new ExplorerTabs(EXPLORER_TAB_ACTIONS_AND_PROCEDURES_FUNCTIONS, id, "Functions", "functions", "Y"));
			break;
		case HOME_TAB_PROVIDERS:
			ret.add(new ExplorerTabs(EXPLORER_TAB_PROVIDERS_DATASOURCES, id, "DataSources", "datasources", "Y"));
			ret.add(new ExplorerTabs(EXPLORER_TAB_PROVIDERS_NOTIFIERS, id, "Notifiers", "notifiers", "Y"));
			break;
		case HOME_TAB_USERS_AND_GROUPS:
			ret.add(new ExplorerTabs(EXPLORER_TAB_USERS_AND_GROUPS_USERS, id, "Users", "users", "Y"));
			ret.add(new ExplorerTabs(EXPLORER_TAB_USERS_AND_GROUPS_GROUPS, id, "Groups", "groups", "Y"));
			break;
		}		
		return ret;
	}

	public PanelTabsList getTabsForPanel(String panelType,String admin)
	{
		System.out.println("getTabsForPanel("+panelType+")");
		PanelTabsList ret = new PanelTabsList();
		boolean AdminMode = (admin != null) && admin.equalsIgnoreCase("y");
		
		System.out.println("AdminMode="+AdminMode);
		if (panelType.equalsIgnoreCase("env"))
		{
			if (AdminMode) ret.add(new PanelTabs("General","general"));
			ret.add(new PanelTabs("Calendar","calendar"));					// calendar is rendered in page
			ret.add(new PanelTabs("Servers","servers"));
			if (AdminMode)
			{
				ret.add(new PanelTabs("Attributes","atts"));
				ret.add(new PanelTabs("Access","access"));
				ret.add(new PanelTabs("Reports","reports"));
				ret.add(new PanelTabs("Availability","avail"));
			}
			ret.add(new PanelTabs("Applications","apps"));
			ret.add(new PanelTabs("History","history"));
		}
		return ret;
	}
	
	public HomeTabsList getHomeTabs()
	{
		HomeTabsList ret = new HomeTabsList();
		ret.add(new HomeTabs(HOME_TAB_WORKBENCH, false, "workbench-icon", "Workbench"));
		if (m_EndPointsTab) {
			ret.add(new HomeTabs(HOME_TAB_ENDPOINTS_AND_CREDENTIALS, true, "endpoints-icon", "Endpoints &amp;<br>Credentials"));
		}
		if (m_ApplicationsTab) {
			ret.add(new HomeTabs(HOME_TAB_APPLICATIONS_AND_COMPONENTS, true, "applications-icon", "Applications &amp;<br>Components"));
		}
		if (m_ActionsTab) {
			ret.add(new HomeTabs(HOME_TAB_ACTIONS_AND_PROCEDURES, true, "actions-icon", "Actions &amp;<br>Procedures"));
		}
		if (m_ProvidersTab) {
			ret.add(new HomeTabs(HOME_TAB_PROVIDERS, true, "providers-icon", "Providers"));
		}
		if (m_UsersTab) {
			ret.add(new HomeTabs(HOME_TAB_USERS_AND_GROUPS, true, "users-icon", "Users &amp;<br>Groups"));
		}
		return ret;
	}
	
	private int parseInt(String s)
	{
		try
		{
			int ret = Integer.parseInt(s);
			return ret;
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}
	
	public void UpdateFragAttrs(Map<String, String> keyvals)
	{
		String title="";
		String summary="";
		int actionid=-1;
		int windowid=-1;
		//int n=0;
		ArrayList<Integer> ids = new ArrayList<Integer>();
		ArrayList<String> vals = new ArrayList<String>();

		Iterator<Map.Entry<String,String>> it = keyvals.entrySet().iterator();

		while (it.hasNext())
		{
			Map.Entry<String,String> pairs = it.next();
			String key = pairs.getKey();
			String val = pairs.getValue();
			System.out.println("UpdateFragAttrs) key=["+key+"] val=["+val+"]");
			if (key.equalsIgnoreCase("title"))   {title=val;/*n++;*/}
			if (key.equalsIgnoreCase("summary")) {summary=val;/*n++;*/}
			if (key.equalsIgnoreCase("a"))       {actionid=parseInt(val);/*n++;*/}
			if (key.equalsIgnoreCase("w"))       {windowid=parseInt(val);/*n++;*/}
			if (key.charAt(0)=='f')
			{
				int attrid = parseInt(key.substring(1));
				if (attrid > 0)
				{
					// f<attrid> .. add to update list
					ids.add(new Integer(attrid));
					vals.add(val);
				}
			}
		}
		try
		{
			Action action = getAction(actionid,false);
			ArchiveAction(action);
			updateModTime(action);
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_actionfrags SET title=?,summary=? WHERE actionid=? AND windowid=?");
			st.setString(1,title);
			st.setString(2,summary);
			st.setInt(3, actionid);
			st.setInt(4,windowid);
			st.execute();
			st.close();
			PreparedStatement st2 = getDBConnection().prepareStatement("DELETE FROM dm.dm_actionfragattrs WHERE actionid=? AND windowid=?");
			st2.setInt(1, actionid);
			st2.setInt(2, windowid);
			st2.execute();
			PreparedStatement st3 = getDBConnection().prepareStatement("INSERT INTO dm.dm_actionfragattrs(actionid,windowid,attrid,value) VALUES (?,?,?,?)");
			st3.setInt(1, actionid);
			st3.setInt(2, windowid);
			for (int t=0;t<ids.size();t++)
			{
				st3.setInt(3,ids.get(t));
				st3.setString(4, vals.get(t));
				st3.execute();
			}
			st3.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Update Step Attributes in Database");
	}
	
	public String getFragmentType(int actionid,int windowid)
	{
		String res="";
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("SELECT a.name FROM dm.dm_fragments a,dm.dm_actionfrags b WHERE a.id=b.typeid AND b.actionid=? AND b.windowid=?");
			st.setInt(1, actionid);
			st.setInt(2,windowid);
			ResultSet rs = st.executeQuery();
			rs.next();
			res = rs.getString(1);
			rs.close();
			st.close();
			return res;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Retrieve fragment type from database");
	}
	
	public void AddFlow(int actionid,String fn,String tn,int pos)
	{
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_actionflows WHERE actionid="+actionid+" AND nodefrom="+fn+" AND nodeto="+tn);
			rs.next();
			int c = rs.getInt(1);
			if (c == 0)
			{
				//
				// This must be a new connection flowid and pos set to 1 for now
				//
				Action action = getAction(actionid,false);
				ArchiveAction(action);
				updateModTime(action);
				RecordObjectUpdate(action,"Connection Added");
				String flowid="1";
				st.execute("INSERT INTO dm.dm_actionflows(actionid,flowid,nodefrom,nodeto,pos) VALUES("+actionid+","+flowid+","+fn+","+tn+","+pos+")");
				getDBConnection().commit();
				
			}
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Add Flow to database");
	}
	
	public void DeleteFlow(int actionid,String fn,String tn)
	{
		Action action = getAction(actionid,false);
		ArchiveAction(action);
		updateModTime(action);
		RecordObjectUpdate(action,"Connection Removed");
		try
		{
			Statement st = getDBConnection().createStatement();
			st.execute("DELETE FROM dm.dm_actionflows WHERE actionid="+actionid+" AND nodefrom="+fn+" AND nodeto="+tn);
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Add Flow to database");
	}
	
	private void internalDeleteNode(Action action,int windowid) throws SQLException
	{
		ArchiveAction(action);
		updateModTime(action);

		System.out.println("DeleteNode("+action.getId()+","+windowid+")");
		PreparedStatement st1 = getDBConnection().prepareStatement("SELECT windowid FROM dm.dm_actionfrags WHERE actionid=? AND parentwindowid=?");
		st1.setInt(1,action.getId());
		st1.setInt(2,windowid);
		ResultSet rs1 = st1.executeQuery();
		while (rs1.next()) {
			// Recurse
			internalDeleteNode(action,rs1.getInt(1));
		}
		rs1.close();
		st1.close();
		Statement st2 = getDBConnection().createStatement();
		st2.execute("DELETE FROM dm.dm_actionfragattrs WHERE actionid="+action.getId()+" AND windowid="+windowid);
		st2.execute("DELETE FROM dm.dm_actionfrags WHERE actionid="+action.getId()+" AND windowid="+windowid);
		//
		// delete any flows to this node (do this as two statements since OR sometimes causes long query)
		//
		st2.execute("DELETE FROM dm.dm_actionflows WHERE actionid="+action.getId()+" AND nodefrom="+windowid);
		st2.execute("DELETE FROM dm.dm_actionflows WHERE actionid="+action.getId()+" AND nodeto="+windowid);
		getDBConnection().commit();
		return;
	}
	
	public void DeleteNode(int actionid,int windowid)
	{
		System.out.println("Delete Node actionid="+actionid+" windowid="+windowid);
		Action action = getAction(actionid,false);
		String sql = "select b.name from dm.dm_actionfrags a,dm.dm_fragments b where a.actionid=? and a.windowid=? and a.typeid=b.id";
		try {
			PreparedStatement ps = getDBConnection().prepareStatement(sql);
			ps.setInt(1,actionid);
			ps.setInt(2,windowid);
			ResultSet rsx = ps.executeQuery();
			System.out.println(sql);
			if (rsx.next()) {
				System.out.println("got a row");
				RecordObjectUpdate(action,"Activity \""+rsx.getString(1)+"\" Deleted from Workflow");
			}
			rsx.close();
			ps.close();
			internalDeleteNode(action,windowid);
		} catch (SQLException ex) {
			ex.printStackTrace();
			rollback();
			throw new RuntimeException("Unable to Delete Node from database");
		}
	}
	
	public String MoveNode(int actionid,int windowid,int pw,int xpos,int ypos,int typeid)
	{
		String qs1="SELECT count(*) FROM dm.dm_actionfrags WHERE actionid="+actionid+" AND windowid="+windowid+" AND parentwindowid is NULL";
		String qs2="SELECT count(*) FROM dm.dm_actionfrags WHERE actionid="+actionid+" AND windowid="+windowid+" AND parentwindowid="+pw;
		String us1="UPDATE dm.dm_actionfrags SET xpos=" + xpos + ", ypos=" + ypos + ", typeid = " + typeid + " WHERE actionid="+actionid+" AND windowid="+windowid+" AND parentwindowid is NULL";
		String us2="UPDATE dm.dm_actionfrags SET xpos=" + xpos + ", ypos=" + ypos + ", typeid = " + typeid + " WHERE actionid="+actionid+" AND windowid="+windowid+" AND parentwindowid="+pw;
		String otid="";
		try
		{
			System.out.println("moving window");
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery(pw>0?qs2:qs1);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("actionid="+actionid+" windowid="+windowid+" pw="+pw+" c="+c);
			if (c>0)
			{
				// This action fragment already exists
				System.out.println("Updating position");
				st.execute(pw>0?us2:us1);
			}
			else
			{
				// New action fragment
				Action action = getAction(actionid,false);
				ArchiveAction(action);
				updateModTime(action);
				PreparedStatement ps = getDBConnection().prepareStatement("SELECT name FROM dm.dm_fragments WHERE id=?");
				ps.setInt(1,typeid);
				ResultSet rs1 = ps.executeQuery();
				if (rs1.next()) {
					RecordObjectUpdate(action,"Activity \""+rs1.getString(1)+"\" added to Workflow");
				}
				rs1.close();
				ps.close();
				System.out.println("Inserting new windowid="+windowid+" pw="+pw);
				st.execute("INSERT INTO dm.dm_actionfrags(actionid,windowid,xpos,ypos,typeid,parentwindowid) VALUES("+actionid+","+windowid+","+xpos+","+ypos+","+ typeid + "," + (pw>0?pw:"NULL") + ")");	
				String getactsql="SELECT actionid,functionid FROM dm.dm_fragments WHERE id=?";
				PreparedStatement actstmt = getDBConnection().prepareStatement(getactsql);
				actstmt.setInt(1,typeid);
				ResultSet actrs = actstmt.executeQuery();
				
				if (actrs.next()) {
					int kind=0;
					int procid = getInteger(actrs,1,0);
					int funcid = getInteger(actrs,2,0);
					if (procid>0 || funcid>0) {
						// Need to get the kind for the otid
						PreparedStatement kindstmt=getDBConnection().prepareStatement("SELECT kind FROM dm.dm_action WHERE id=?");
						kindstmt.setInt(1, procid>0?procid:funcid);
						ResultSet kindrs = kindstmt.executeQuery();
						if (kindrs.next()) {
							kind = kindrs.getInt(1);
						}
						kindrs.close();
						kindstmt.close();
					}
					if (procid>0) otid="pr"+procid+"-"+kind;
					else
					if (funcid>0) otid="fn"+funcid+"-"+kind;
				}
				actrs.close();
				actstmt.close();
			}
			st.close();
			getDBConnection().commit();
			return otid;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Add Flow to database");
		
	}
	
	
	public void MoveAppComponentStart(int appid,int xpos)
	{
		try
		{
			System.out.println("moving component start window");
			Statement st = getDBConnection().createStatement();
			st.execute("UPDATE dm.dm_application SET startx=" + xpos+" where id="+appid);
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to move component start window in database");
	}
	
	
	public void MoveServer(int envid,int serverid,int xpos,int ypos)
	{
		try
		{
			System.out.println("moving server");
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_serversinenv WHERE envid="+envid+" AND serverid="+serverid);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("envid="+envid+" serverid="+serverid+" c="+c);
			if (c>0)
			{
				// This server already already exists
				System.out.println("Updating position");
				st.execute("UPDATE dm.dm_serversinenv SET xpos=" + xpos + ", ypos=" + ypos + " WHERE envid="+envid+" AND serverid="+serverid);
			}
			else
			{
				// New server in environment
				System.out.println("Inserting new server");
				st.execute("INSERT INTO dm.dm_serversinenv(envid,serverid,xpos,ypos) VALUES("+envid+","+serverid+","+xpos+","+ypos + ")");	
				st.execute("UPDATE dm.dm_environment SET modifierid="+getUserID()+", modified="+timeNow()+" WHERE id="+envid);
				// Environment env = getEnvironment(envid,false);
				// Server server = getServer(serverid,false);
			}
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Add End Point to database");
	}
	
	public void NotifyServersAddedToEnvironment(int envid, String sl)
	{
		// ServerList (sl) passed in from UpdateAttrs in format id,id,id...
		System.out.println("NotifyServersAddedToEnvironment(envid="+envid+", sl="+sl);
		Environment env = getEnvironment(envid,false);
		System.out.println("envid="+envid+" sl="+sl);
		String[] servers = sl.split("x");
		String ServerList="";
		System.out.println("servers.length="+servers.length);
		int singleid=0;
		for (int i=0;i<servers.length;i++) {
			int servid = Integer.parseInt(servers[i]);
			Server server = getServer(servid,false);
			System.out.println("server.name="+server.getName());
			// Add notification to server timeline
			String linkval2="<a href='javascript:SwitchDisplay(\"en"+env.getId()+"\");'><b>"+env.getName()+"</b></a>";
			RecordObjectUpdate(server,"Added to Environment "+linkval2,envid);
			String linkval="<a href='javascript:SwitchDisplay(\"se"+server.getId()+"\");'><b>"+server.getName()+"</b></a>";
			if (i>0) ServerList=ServerList+", ";
			ServerList=ServerList+linkval;
			singleid=server.getId();
		}
		System.out.println("ServerList="+ServerList);
		// Now update the environment's timeline
		if (servers.length>1) {
			RecordObjectUpdate(env,"Servers "+ServerList+" Added to Environment");
		} else {
			RecordObjectUpdate(env,"Server "+ServerList+" Added to Environment",singleid);
		}
		try {
			getDBConnection().commit();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
	
	public void applicationMoveVersion(int appid,int verid,int xpos,int ypos)
	{
		try
		{
			System.out.println("moving version");
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_application WHERE parentid="+appid+" AND id="+verid);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("verid="+verid+" appid="+appid+" c="+c);
			if (c>0)
			{
				// This version already exists
				System.out.println("Updating position");
				st.execute("UPDATE dm.dm_application SET xpos=" + xpos + ", ypos=" + ypos + " WHERE parentid="+appid+" AND id="+verid);
			}
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Add End Point to database");
	}
	
	private void AddComponents(long t,int appid,int predecessorid, boolean isRelease)
		throws SQLException
	{
		String sql1 =	null;
		if (isRelease)
		sql1 = "INSERT INTO dm.dm_applicationcomponent(appid,childappid,xpos,ypos)	"
					+ 	"SELECT ?,childappid,xpos,ypos		"
					+	"FROM dm.dm_applicationcomponent	"
					+ 	"WHERE appid=?";
		else
	  sql1 = "INSERT INTO dm.dm_applicationcomponent(appid,compid,xpos,ypos) "
     +  "SELECT ?,compid,xpos,ypos  "
     + "FROM dm.dm_applicationcomponent "
     +  "WHERE appid=?";
		
		String sql2 =	"INSERT INTO dm.dm_applicationcomponentflows(appid,objfrom,objto) "
					+ 	"SELECT ?,objfrom,objto				"
					+	"FROM dm.dm_applicationcomponentflows	"
					+ 	"WHERE appid=?";
		
		PreparedStatement psx = getDBConnection().prepareStatement(sql1);
		psx.setInt(1,appid);
		psx.setInt(2,predecessorid);
		psx.execute();
		psx.close();
		PreparedStatement psy = getDBConnection().prepareStatement(sql2);
		psy.setInt(1,appid);
		psy.setInt(2,predecessorid);
		psy.execute();
		psy.close();
	}
	
	public int applicationNewVersion(int appid,int xpos,int ypos, boolean isRelease)
	{
		try
		{
			System.out.println("applicationNewVersion("+appid+")");
			long t = timeNow();
			System.out.println("new version");
			Application app  = getApplication(appid,true);
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_application WHERE parentid="+appid+" AND status='N'");
			rs.next();
			int c = rs.getInt(1)+1;
			System.out.println("c="+c);
			rs.close();
			String NewName=app.getName()+";"+c;
			System.out.println("NewName="+NewName);
			int newid = getID("application");
			int domainid = app.getDomainId();
			if (isRelease)
				CreateNewObject("relversion",NewName,domainid,appid,newid,"");
			else
				CreateNewObject("appversion",NewName,domainid,appid,newid,"");
			st.execute("UPDATE dm.dm_application SET xpos=" + xpos + ", ypos=" + ypos + " WHERE parentid="+appid+" AND id="+newid);
			st.execute("UPDATE dm.dm_application SET modified="+t+",modifierid="+getUserID()+" WHERE id="+newid);
			Datasource ds = app.getDatasource();
			if (ds != null) {
				st.execute("UPDATE dm.dm_application SET datasourceid="+ds.getId()+" WHERE id="+newid);
			}
			st.close();
			//
			// When first created, the components should be copied from the "base" application. When the predecessor is changed
			// through the editor, the compoments should be erased and copied from the new predecessor.
			//
			AddComponents(t,newid,appid, isRelease);
			
			Application newapp = getApplication(newid,true);
			AddApplicationAttribs(t,newapp,app);
   			
			getDBConnection().commit();
			return newid;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create New Version in database");
	}
	
 public void AddApplicationAttribs(long t, Application newapp, Application app)
 {
  List<DMAttribute> attribs = getAttributesForObject(app);
  AttributeChangeSet changes = new AttributeChangeSet();
  
  for (DMAttribute a : attribs)
  {
   changes.addAdded(new DMAttribute(a.getName(), a.getValue()));
  } 
  newapp.updateAttributes(changes);
 }
 
	public void componentMoveVersion(int compid,int verid,int xpos,int ypos)
	{
		try
		{
			System.out.println("moving component version");
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_component WHERE id="+verid);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("verid="+verid+" compid="+compid+" c="+c);
			if (c>0)
			{
				// This version already exists
				System.out.println("Updating position");
				st.execute("UPDATE dm.dm_component SET xpos=" + xpos + ", ypos=" + ypos + " WHERE id="+verid);
			}
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Move Component Position in database");
	}
	
	
	
	public void componentItemMoveItem(int compid,int itemid,int xpos,int ypos)
	{
		try
		{
			System.out.println("moving component item compid="+compid+" itemid="+itemid);
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_componentitem WHERE compid="+compid+" AND id="+itemid);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("c="+c);
			if (c>0)
			{
				// This version already exists
				System.out.println("Updating position of component item xpos="+xpos+" ypos="+ypos);
				st.execute("UPDATE dm.dm_componentitem SET xpos=" + xpos + ", ypos=" + ypos + " WHERE compid="+compid+" AND id="+itemid);
			}
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Move Component Item Position in database");
	}
	
	/*
	public void componentApplicationMoveItem(int compid,int appid,int xpos,int ypos)
	{
		try
		{
			System.out.println("moving component for application compid="+compid+" appid="+appid);
			Statement st = m_conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_applicationcomponent WHERE compid="+compid+" AND appid="+appid);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("c="+c);
			if (c>0)
			{
				// This version already exists
				System.out.println("Updating position of component for application xpos="+xpos+" ypos="+ypos);
				st.execute("UPDATE dm.dm_applicationcomponent SET xpos=" + xpos + ", ypos=" + ypos + " WHERE compid="+compid+" AND appid="+appid);
			}
			st.close();
			m_conn.commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Move Component Position for Application in database");
	}
	*/

	public void addComponentToServer(int servid,int compid)
	{
		try
		{
			// If this is a component VERSION add the component BASE
			Statement st1 = getDBConnection().createStatement();
			ResultSet rs1 = st1.executeQuery("SELECT parentid FROM dm.dm_component WHERE id="+compid);
			rs1.next();
			int parentid = getInteger(rs1,1,0);
			if (parentid > 0) compid=parentid;
			rs1.close();
			
			System.out.println("adding component compid="+compid+" to server servid="+servid);
			Statement st2 = getDBConnection().createStatement();
			ResultSet rs2 = st2.executeQuery("SELECT count(*) FROM dm.dm_compsallowedonserv WHERE compid="+compid+" AND serverid="+servid);
			rs2.next();
			int c = rs2.getInt(1);
			rs2.close();
			System.out.println("c="+c);
			if (c==0)
			{
				// New component on this server
				System.out.println("adding component to server");
				PreparedStatement ps = getDBConnection().prepareStatement("INSERT INTO dm.dm_compsallowedonserv(serverid,compid) VALUES(?,?)");
				ps.setInt(1,servid);
				ps.setInt(2,compid);
				ps.execute();
				ps.close();
				getDBConnection().commit();
			}
			st2.close();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Add Component for Server in database");
	}
	
	public void addComponentVersionToServer(int servid,int compid)
	{
		try
		{
			// Component comp = getComponent(compid,true);
			// int parentid = comp.getParentId();
			System.out.println("adding component version compid="+compid+" to server servid="+servid);
			// addComponentToServer(servid,compid);	// Add component BASE to components allowed on server
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_compsonserv WHERE compid="+compid+" AND serverid="+servid);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("c="+c);
			if (c==0)
			{
				// This component VERSION is not present on this server
				long t = timeNow();
				String updsql = "UPDATE dm.dm_compsonserv SET compid=?,deploymentid=NULL,modifierid=?,modified=? WHERE serverid=? AND (compid IN (SELECT parentid FROM dm.dm_component WHERE id=?) OR compid=?)";
				PreparedStatement updstmt = getDBConnection().prepareStatement(updsql);
				updstmt.setInt(1,compid);
				updstmt.setInt(2, getUserID());
				updstmt.setLong(3,t);
				updstmt.setInt(4,servid);
				updstmt.setInt(5,compid);
				updstmt.setInt(6,compid);
				updstmt.execute();
				int updcount = updstmt.getUpdateCount();
				updstmt.close();
				System.out.println("upcount="+updcount);
				if (updcount == 0) {
					// New component version on this server
					System.out.println("adding component version to server");
					PreparedStatement ps2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_compsonserv(compid,serverid,modifierid,modified) VALUES(?,?,?,?)");
					ps2.setInt(1,compid);
					ps2.setInt(2,servid);
					ps2.setInt(3, getUserID());
					ps2.setLong(4,t);
					ps2.execute();
					ps2.close();
				}
				getDBConnection().commit();
			}
			st.close();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Add Component Version to Server in database");
	}
	
	public void addComponentToApplication(int appid,int compid,int xpos,int ypos, boolean isRelease)
	{
		Application app = getApplication(appid,true);
		try
		{
			System.out.println("adding component compid="+compid+" to application appid="+appid);
			Statement st = getDBConnection().createStatement();
			ResultSet rs = null;
			if (isRelease) {
				rs = st.executeQuery("SELECT count(*) FROM dm.dm_applicationcomponent WHERE childappid="+compid+" AND appid="+appid);
			} else {
				rs = st.executeQuery("SELECT count(*) FROM dm.dm_applicationcomponent WHERE compid="+compid+" AND appid="+appid);
			}
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("c="+c);
			if (c>0)
			{
				// This component already exists - just update the x and y co-ordinates
				System.out.println("Updating position of component for application xpos="+xpos+" ypos="+ypos);
				if (isRelease) {
					st.execute("UPDATE dm.dm_applicationcomponent SET xpos=" + xpos + ", ypos=" + ypos + " WHERE childappid="+compid+" AND appid="+appid);
				} else {
					st.execute("UPDATE dm.dm_applicationcomponent SET xpos=" + xpos + ", ypos=" + ypos + " WHERE compid="+compid+" AND appid="+appid);
				}
			}
			else
			{
				// New component for this application
				System.out.println("adding component to application");
				PreparedStatement ps = null;
				
				if (isRelease) {
					Application childapp = getApplication(compid,true);
					String at=(childapp.getParentId()>0)?"av":"ap";
					String linkval="<a href='javascript:SwitchDisplay(\"rl"+app.getId()+"\");'><b>"+app.getName()+"</b></a>";
					String linkval2="<a href='javascript:SwitchDisplay(\""+at+childapp.getId()+"\");'><b>"+childapp.getName()+"</b></a>";
					RecordObjectUpdate(childapp,"Added to Release "+linkval,appid);
					RecordObjectUpdate(app, "Added Application "+linkval2,compid);
					ps = getDBConnection().prepareStatement("INSERT INTO dm.dm_applicationcomponent(appid,childappid,xpos,ypos) VALUES(?,?,?,?)");
				} else {
					Component comp = getComponent(compid,true);
					String at=(app.getParentId()>0)?"av":"ap";
					String ct=(comp.getParentId()>0)?"cv":"co";
					String linkval="<a href='javascript:SwitchDisplay(\""+at+app.getId()+"\");'><b>"+app.getName()+"</b></a>";
					String linkval2="<a href='javascript:SwitchDisplay(\""+ct+comp.getId()+"\");'><b>"+comp.getName()+"</b></a>";
					RecordObjectUpdate(comp,"Added to Application "+linkval,appid);
					RecordObjectUpdate(app, "Added Component "+linkval2,compid);
					ps = getDBConnection().prepareStatement("INSERT INTO dm.dm_applicationcomponent(appid,compid,xpos,ypos) VALUES(?,?,?,?)");
				}
				ps.setInt(1,appid);
				ps.setInt(2,compid);
				ps.setInt(3,xpos);
				ps.setInt(4,ypos);
				ps.execute();
				ps.close();
				getDBConnection().commit();
			}
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Move Component Position for Application in database");
	}
	
	/*
	public void componentServerMoveItem(int compid,int servid,int xpos,int ypos)
	{
		try
		{
			System.out.println("moving component for server compid="+compid+" servid="+servid);
			Statement st = m_conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_compsallowedonserv WHERE compid="+compid+" AND serverid="+servid);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("c="+c);
			if (c>0)
			{
				// This version already exists
				System.out.println("Updating position of component for server xpos="+xpos+" ypos="+ypos);
				st.execute("UPDATE dm.dm_compsallowedonserv SET xpos=" + xpos + ", ypos=" + ypos + " WHERE compid="+compid+" AND serverid="+servid);
			}
			st.close();
			m_conn.commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Move Component Position for Server in database");
	}
	*/
	
	private void DeleteComponentItems(int compid) throws SQLException
	{
		System.out.println("DeleteComponentItems from compid="+compid);
		PreparedStatement psx = getDBConnection().prepareStatement("DELETE FROM dm.dm_compitemprops WHERE compitemid in (SELECT id FROM dm.dm_componentitem WHERE compid=?)");
		psx.setInt(1,compid);
		psx.execute();
		psx.close();
		PreparedStatement psy = getDBConnection().prepareStatement("DELETE FROM dm.dm_componentitem WHERE compid=?");
		psy.setInt(1,compid);
		psy.execute();
		psy.close();
	}
	
	private void DeleteComponentAttribs(int compid) throws SQLException
	{
		System.out.println("DeleteComponentAttribs from compid="+compid);
		PreparedStatement psx = getDBConnection().prepareStatement("DELETE FROM dm.dm_componentvars WHERE compid=?");
		psx.setInt(1,compid);
		psx.execute();
		psx.close();
	}
	
	public void AddComponentItems(long t,int compid,int predecessorid) throws SQLException
	{
		List<ComponentItem> cis = getComponentItems(predecessorid);	// original component items
		System.out.println("cis.size()="+cis.size());
		HashMap<Integer, Integer> idmap = new HashMap<Integer, Integer>(cis.size());
		int n = getID("componentitem");
		// Create a mapping table from old id to new id
		for (ComponentItem ci: cis)
		{
			System.out.println("Mapping "+ci.getId()+" to "+n);
			idmap.put(ci.getId(),n);
			n++;
		}
		for (ComponentItem ci: cis)
		{
			String sql =	"INSERT INTO dm.dm_componentitem(id,name,summary,compid,repositoryid,target,"
						+ 	"predecessorid,xpos,ypos,creatorid,created,modifierid,modified,status,rollup,rollback)	"
						+ 	"SELECT ?,name,summary,?,repositoryid,target,"
						+	"?,xpos,ypos,?,?,?,?,'N', rollup, rollback	"
						+	"FROM dm.dm_componentitem	"
						+ 	"WHERE id=? AND status='N'";
		
			PreparedStatement psx = getDBConnection().prepareStatement(sql);
			psx.setInt(1,idmap.get(ci.getId()));
			psx.setInt(2,compid);
			int pi = ci.getPredecessorId();
			if (pi>0) psx.setInt(3,idmap.get(pi));
			else psx.setNull(3,Types.INTEGER);
			psx.setInt(4,getUserID());
			psx.setLong(5,t);
			psx.setInt(6,getUserID());
			psx.setLong(7,t);
			psx.setInt(8,ci.getId());
			psx.execute();
			psx.close();
		}
		for (ComponentItem ci: cis)
		{
			String sql =	"INSERT INTO dm.dm_compitemprops(compitemid,name,value,encrypted) "
						+ 	"SELECT ?,name,value,encrypted	"
						+	"FROM dm.dm_compitemprops	"
						+ 	"WHERE compitemid=?";
		
			PreparedStatement psy = getDBConnection().prepareStatement(sql);
			psy.setInt(1,idmap.get(ci.getId()));
			psy.setInt(2,ci.getId());
			psy.execute();
			psy.close();
		}
		setID("componentitem",n);
		getDBConnection().commit();
	}
	
	public int componentNewVersion(int compid,int xpos,int ypos)
	{
		try
		{
			long t = timeNow();
			System.out.println("new version");
			Component comp  = getComponent(compid,true);
			Component basecomp = null;
			int parentid;
			if (comp.getParentId()>0) {
				// This is a component version - get the parent
				basecomp = getComponent(comp.getParentId(),true);
				parentid = basecomp.getId();
			} else {
				parentid = compid;
			}
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_component WHERE status='N' AND parentid="+parentid);
			rs.next();
			int c = rs.getInt(1)+1;
			rs.close();
			
			String NewName=(basecomp != null)?(basecomp.getName()+";"+c):(comp.getName()+";"+c);
			int newid = getID("component");
			int domainid = comp.getDomainId();
			CreateNewObject("component",NewName,domainid,compid,newid,"");
			System.out.println("Updating dm_component where id="+newid);
			st.execute("UPDATE dm.dm_component SET predecessorid=" + compid + ", parentid=" + parentid + ", xpos=" + xpos + ", ypos=" + ypos + " WHERE id="+newid);
			st.execute("UPDATE dm.dm_component SET modified="+t+",modifierid="+getUserID()+" WHERE id="+compid);
			st.execute("UPDATE dm.dm_component SET comptypeid="+comp.getComptypeId() + " WHERE id="+newid);

			if (comp.getPreAction() != null)
				st.execute("UPDATE dm.dm_component SET preactionid=" + comp.getPreAction().getId() + " WHERE id="+newid);

			if (comp.getPostAction() != null)
				st.execute("UPDATE dm.dm_component SET postactionid=" + comp.getPostAction().getId() + " WHERE id="+newid);
			
			if (comp.getCustomAction() != null)
			    st.execute("UPDATE dm.dm_component SET actionid=" + comp.getCustomAction().getId() + " WHERE id="+newid);
			
			if (comp.getBaseDirectory() != null)
				st.execute("UPDATE dm.dm_component SET basedir='"+comp.getBaseDirectory()+"' WHERE id="+newid);
			
			if (comp.getBuildJob() != null) 
				st.execute("UPDATE dm.dm_component SET buildjobid=" + comp.getBuildJob().getId()+",lastbuildnumber="+comp.getLastBuildNumber()+" WHERE id="+newid);
			
			if (comp.getDatasource() != null)
				st.execute("UPDATE dm.dm_component SET datasourceid="+ comp.getDatasource().getId()+" WHERE id="+newid);
			
			//
			// When first created, the component items should be copied from the "base" component. When the predecessor is changed
			// through the editor, the compoment items should be erased and copied from the new predecessor.
			//
			AddComponentItems(t,newid,compid);

			Component newcomp = getComponent(newid,true);
			AddComponentAttribs(newcomp,comp);
			st.execute("INSERT INTO dm.dm_component_categories(id,categoryid) SELECT "+newid+",categoryid FROM dm_component_categories WHERE id="+compid);
			st.close();
			
			getDBConnection().commit();
			return newid;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create New Component Version in database");
	}
	
	public void AddComponentAttribs(Component newcomp, Component comp)
	{
		List<DMAttribute> attribs = getAttributesForObject(comp);
		AttributeChangeSet changes = new AttributeChangeSet();
  
		for (DMAttribute a : attribs) {
			changes.addAdded(new DMAttribute(a.getName(), a.getValue()));
		} 
		newcomp.updateAttributes(changes);
	}
	
	public int componentItemNewItem(int compid,int xpos,int ypos)
	{
		try
		{
   int pred = -1;
			System.out.println("new component item");
			Component comp  = getComponent(compid,true);
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_componentitem WHERE compid="+compid);
			rs.next();
			int c = rs.getInt(1)+1;
			rs.close();
			
			String comptype = null;
   Statement st2 = getDBConnection().createStatement();
   ResultSet rs2 = st2.executeQuery("SELECT database FROM dm.dm_component a, dm.dm_type b WHERE a.comptypeid = b.id and a.id="+compid);
   while (rs2.next())
   { 
    comptype = rs2.getString(1);
   }
   rs2.close();
   
   if (comptype == null)
   { 
    comptype = "Item "+c;
   } 
   else
   { 
    if (comptype.equalsIgnoreCase("Y"))
    { 
     if (c == 1)
     {
      comptype = "Roll Forward";
      String NewName=comptype;
      int itemid = getID("componentitem");
      int domainid = comp.getDomainId();
      CreateNewObject("componentitem",NewName,domainid,compid,itemid,"");
      st.execute("UPDATE dm.dm_componentitem SET rollup=1, xpos=" + xpos + ", ypos=" + ypos + " WHERE compid="+compid+" AND id="+itemid);
      st.execute("UPDATE dm.dm_component SET modified="+timeNow()+",modifierid="+getUserID()+" WHERE id="+compid);
      pred = itemid;
      
      comptype = "Rollback";
      NewName=comptype;
      itemid = getID("componentitem");
      domainid = comp.getDomainId();
      ypos += 200;
      CreateNewObject("componentitem",NewName,domainid,compid,itemid,"");
      st.execute("UPDATE dm.dm_componentitem SET rollback=1, predecessorid=" + pred + ", xpos=" + xpos + ", ypos=" + ypos + " WHERE compid="+compid+" AND id="+itemid);
      st.execute("UPDATE dm.dm_component SET modified="+timeNow()+",modifierid="+getUserID()+" WHERE id="+compid);
      st.close();
      getDBConnection().commit();
      return pred;
     }
     else
     { 
      comptype = "Database "+c;
     }
		  }    
    else
    { 
     comptype = "Item "+c;
    } 
   } 
  
			String NewName=comptype;
			int itemid = getID("componentitem");
			int domainid = comp.getDomainId();
			CreateNewObject("componentitem",NewName,domainid,compid,itemid,"");
			st.execute("UPDATE dm.dm_componentitem SET xpos=" + xpos + ", ypos=" + ypos + " WHERE compid="+compid+" AND id="+itemid);
			st.execute("UPDATE dm.dm_component SET modified="+timeNow()+",modifierid="+getUserID()+" WHERE id="+compid);
   st.close();
			getDBConnection().commit();
			return itemid;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create New Component Item in database");
	}
	
	
	public int applicationNewComponent(int appid,int xpos,int ypos, boolean isRelease)
	{
		try
		{
			System.out.println("new component in application");
			Application app  = getApplication(appid,true);
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_applicationcomponent WHERE appid="+appid);
			rs.next();
			int c = rs.getInt(1)+1;
			rs.close();
			String NewName="Component "+c;
			int compid = getID("component");
			int domainid = app.getDomainId();
			CreateNewObject("component",NewName,domainid,0,compid,"");
			// 06/01/2014 - always run from diagram background menu, so predecessor is never known - removed predecessorid and ? and setInt(3,appid);
			PreparedStatement ps = null;
			if (isRelease)
			 ps = getDBConnection().prepareStatement("INSERT INTO dm.dm_applicationcomponent(appid,childappid,xpos,ypos) VALUES(?,?,?,?)");
			else
    ps = getDBConnection().prepareStatement("INSERT INTO dm.dm_applicationcomponent(appid,compid,xpos,ypos) VALUES(?,?,?,?)");

			ps.setInt(1,appid);
			ps.setInt(2,compid);
			ps.setInt(3,xpos);
			ps.setInt(4,ypos);
			ps.execute();
			st.close();
			getDBConnection().commit();
			return compid;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create New Component Item in database");
	}
	
	
	public int serverNewComponent(int servid,int xpos,int ypos)
	{
		try
		{
			System.out.println("new component in server");
			Server serv  = getServer(servid,true);
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_compsallowedonserv WHERE serverid="+servid);
			rs.next();
			int c = rs.getInt(1)+1;
			rs.close();
			String NewName="Component "+c;
			int compid = getID("component");
			int domainid = serv.getDomainId();
			CreateNewObject("component",NewName,domainid,0,compid,"");
			// 06/01/2014 - always run from diagram background menu, so predecessor is never known - removed predecessorid and ? and setInt(3,appid);
			PreparedStatement ps = getDBConnection().prepareStatement("INSERT INTO dm.dm_compsallowedonserv(compid,serverid,xpos,ypos) VALUES(?,?,?,?)");
			ps.setInt(1,compid);
			ps.setInt(2,servid);
			ps.setInt(3,xpos);
			ps.setInt(4,ypos);
			ps.execute();
			st.close();
			getDBConnection().commit();
			return compid;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create New Component Item in database");
	}
	
	public void RemoveServerFromEnvironment(int envid,int serverid)
	{
		try
		{
			System.out.println("removing server from environment");
			PreparedStatement st = getDBConnection().prepareStatement("DELETE FROM dm.dm_serversinenv WHERE envid=? AND serverid=?");
			st.setInt(1, envid);
			st.setInt(2, serverid);
			st.execute();
			st.close();
			Server server = getServer(serverid,false);
			Environment env = getEnvironment(envid,false);
			String linkval="<a href='javascript:SwitchDisplay(\"se"+server.getId()+"\");'><b>"+server.getName()+"</b></a>";
			RecordObjectUpdate(env,"Server "+linkval+" Removed from Environment",serverid);
			String linkval2="<a href='javascript:SwitchDisplay(\"en"+env.getId()+"\");'><b>"+env.getName()+"</b></a>";
			RecordObjectUpdate(server,"Removed from Environment "+linkval2,envid);
			updateModTime(env);
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Removed Server from Environment");
	}
	
	public void applicationRemoveVersion(int appid,int verid)
	{
		try
		{
			System.out.println("removing version from application");
			PreparedStatement st = getDBConnection().prepareStatement("DELETE FROM dm.dm_applicationcomponentflows WHERE appid=?");
			st.setInt(1, verid);
			st.execute();
			st.close();
			PreparedStatement st1 = getDBConnection().prepareStatement("DELETE FROM dm.dm_applicationcomponent WHERE appid=?");
			st1.setInt(1, verid);
			st1.execute();
			st1.close();
			PreparedStatement st2 = getDBConnection().prepareStatement("DELETE FROM dm.dm_applicationvars WHERE appid=?");
			st2.setInt(1, verid);
			st2.execute();
			st2.close();
			PreparedStatement st3 = getDBConnection().prepareStatement("DELETE FROM dm.dm_approval WHERE appid=?");
			st3.setInt(1, verid);
			st3.execute();
			st3.close();
			PreparedStatement st4 = getDBConnection().prepareStatement("DELETE FROM dm.dm_appsinenv WHERE appid=?");
			st4.setInt(1, verid);
			st4.execute();
			st4.close();
			PreparedStatement st5 = getDBConnection().prepareStatement("DELETE FROM dm.dm_request WHERE appid=?");
			st5.setInt(1, verid);
			st5.execute();
			st5.close();
			PreparedStatement st6 = getDBConnection().prepareStatement("DELETE FROM dm.dm_calendar WHERE appid=?");
			st6.setInt(1, verid);
			st6.execute();
			st6.close();
			PreparedStatement st7 = getDBConnection().prepareStatement("UPDATE dm.dm_application SET status='D' WHERE id=? AND parentid=?");
			st7.setInt(1, verid);
			st7.setInt(2, appid);
			st7.execute();
			st7.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Remove Version from Application");
	}
	
	public void componentRemoveVersion(int parentcompid,int verid)
	{
		try
		{
		 System.out.println("removing version from component item's props");
   PreparedStatement st = getDBConnection().prepareStatement("DELETE FROM dm.dm_compitemprops a WHERE a.compitemid in (select id from dm.dm_componentitem b where b.compid=?)");
		   st.setInt(1, verid);
		   st.execute();
		   st.close();
 
   System.out.println("removing compnent vars from component");
		   st = getDBConnection().prepareStatement("DELETE FROM dm.dm_componentvars WHERE compid=?");
		   st.setInt(1, verid);
		   st.execute();
		   st.close();
   
   System.out.println("removing version from component items");
		   st = getDBConnection().prepareStatement("DELETE FROM dm.dm_componentitem WHERE compid=?");
		   st.setInt(1, verid);
		   st.execute();
		   st.close();

   System.out.println("removing version from component");
			st = getDBConnection().prepareStatement("DELETE FROM dm.dm_component WHERE id=? AND parentid=?");
			st.setInt(1, verid);
			st.setInt(2, parentcompid);
			st.execute();
			st.close();
	System.out.println("removing from category");

			st = getDBConnection().prepareStatement("DELETE FROM dm.dm_component_categories WHERE id=?");
			st.setInt(1, verid);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Remove Version from Component");
	}
	
	public void componentItemRemoveItem(int compid,int itemid)
	{
		try
		{
			System.out.println("removing version from component");
			PreparedStatement st1 = getDBConnection().prepareStatement("DELETE FROM dm.dm_compitemprops WHERE compitemid=?");
			st1.setInt(1,itemid);
			st1.execute();
			PreparedStatement st2 = getDBConnection().prepareStatement("DELETE FROM dm.dm_componentitem WHERE id=? AND compid=?");
			st2.setInt(1, itemid);
			st2.setInt(2, compid);
			st2.execute();
			st2.close();
			st1.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Remove Item Component");
	}
	
	public void applicationRemoveComponent(int appid,int compid, boolean isRelease)
	{
		Application app = getApplication(appid,true);
		try
		{
			System.out.println("removing component "+compid+" from application "+appid);
			PreparedStatement st1 = getDBConnection().prepareStatement("DELETE FROM dm.dm_applicationcomponentflows WHERE appid=? AND (objfrom=? OR objto=?)");
			st1.setInt(1,appid);
			st1.setInt(2,compid);
			st1.setInt(3,compid);
			st1.execute();
			st1.close();
			PreparedStatement st2 = null;
			if (isRelease) {
				Application childapp = getApplication(compid,true);
				String at=(childapp.getParentId()>0)?"av":"ap";
				String linkval="<a href='javascript:SwitchDisplay(\"rl"+app.getId()+"\");'><b>"+app.getName()+"</b></a>";
				String linkval2="<a href='javascript:SwitchDisplay(\""+at+childapp.getId()+"\");'><b>"+childapp.getName()+"</b></a>";
				RecordObjectUpdate(childapp,"Removed from Release "+linkval,appid);
				RecordObjectUpdate(app, "Removed Application "+linkval2,compid);
				st2 = getDBConnection().prepareStatement("DELETE FROM dm.dm_applicationcomponent WHERE appid=? AND childappid=?");
			} else {
				Component comp = getComponent(compid,true);
				String at=(app.getParentId()>0)?"av":"ap";
				String ct=(comp.getParentId()>0)?"cv":"co";
				String linkval="<a href='javascript:SwitchDisplay(\""+at+app.getId()+"\");'><b>"+app.getName()+"</b></a>";
				String linkval2="<a href='javascript:SwitchDisplay(\""+ct+comp.getId()+"\");'><b>"+comp.getName()+"</b></a>";
				RecordObjectUpdate(comp,"Removed from Application "+linkval,appid);
				RecordObjectUpdate(app, "Removed Component "+linkval2,compid);
				st2 = getDBConnection().prepareStatement("DELETE FROM dm.dm_applicationcomponent WHERE appid=? AND compid=?");
			}
			st2.setInt(1,appid);
			st2.setInt(2,compid);
			st2.execute();
			st2.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Remove Component from Application");
	}
	
	public void serverRemoveComponent(int servid,int compid)
	{
		try
		{
			System.out.println("removing component "+compid+" from server "+servid);
			PreparedStatement st1 = getDBConnection().prepareStatement("DELETE FROM dm.dm_compsallowedonserv WHERE serverid=? AND compid=?");
			st1.setInt(1,servid);
			st1.setInt(2,compid);
			st1.execute();
			st1.close();
			PreparedStatement st2 = getDBConnection().prepareStatement("DELETE FROM dm.dm_compsonserv WHERE serverid=? AND (compid=? OR compid IN (select id FROM dm.dm_component WHERE parentid=?))");
			st2.setInt(1,servid);
			st2.setInt(2,compid);
			st2.setInt(3,compid);
			st2.execute();
			st2.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Remove Component from Server");
	}
	
	public void AddConnector(int envid,int fromnode,int tonode,int fromside,int toside)
	{
		try
		{
			System.out.println("Adding connector envid="+envid+" fromnode="+fromnode+" tonode="+tonode+" fromside="+fromside+" toside="+toside);
			PreparedStatement st = getDBConnection().prepareStatement("SELECT count(*) FROM dm.dm_server_connections WHERE envid=? AND serverfrom=? AND serverto=? AND serverfromedge=? AND servertoedge=?");
			st.setInt(1,envid);
			st.setInt(2,fromnode);
			st.setInt(3,tonode);
			st.setInt(4,fromside);
			st.setInt(5,toside);
			ResultSet rs = st.executeQuery();
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			System.out.println("c="+c);
			if (c == 0)
			{
				// New connection
				PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_server_connections(envid,serverfrom,serverfromedge,serverto,servertoedge) VALUES (?,?,?,?,?)");
				st2.setInt(1,envid);
				st2.setInt(2,fromnode);
				st2.setInt(3,fromside);
				st2.setInt(4,tonode);
				st2.setInt(5,toside);
				st2.execute();
				st2.close();
			}
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Add Connector in database");
	}

	public void applicationAddVersionDependency(int appid,int fromnode,int tonode)
	{
		try
		{
			System.out.println("Adding version dependency");
			
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_application SET predecessorid=? WHERE id=? AND parentid=?");
			st.setInt(1,fromnode);
			st.setInt(2,tonode);
			st.setInt(3,appid);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create Version Dependency in Database");
	}
	
	public void componentAddVersionDependency(int compid,int fromnode,int tonode)
	{
		try
		{
			// Update the predecessor to create the dependency
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_component SET predecessorid=? WHERE id=? AND parentid=?");
			st.setInt(1,fromnode);
			st.setInt(2,tonode);
			st.setInt(3,compid);
			st.execute();
			st.close();
			DeleteComponentItems(tonode);					// Remove the old component items
			AddComponentItems(timeNow(),tonode,fromnode);	// add copy the new component items from the predecessor
			Component newcomp = getComponent(tonode,false);
			Component comp = getComponent(fromnode,false);
			DeleteComponentAttribs(tonode);
			AddComponentAttribs(newcomp,comp);
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create Version Dependency in Database");
	}
	
	
	public void componentItemAddLink(int compid,int fromnode,int tonode)
	{
		try
		{
			System.out.println("Adding component item link");
			
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_componentitem SET predecessorid=? WHERE id=? AND compid=?");
			st.setInt(1,fromnode);
			if (tonode>0)
				st.setInt(2,tonode);
			else 
				st.setNull(2, Types.INTEGER);
			st.setInt(3,compid);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create Component Item Link in Database");
	}
	
	public void applicationComponentAddLink(int appid,int fromnode,int tonode)
	{
		try
		{
			System.out.println("Adding link between components for application");
			
			PreparedStatement stc = getDBConnection().prepareStatement("SELECT count(*) from dm.dm_applicationcomponentflows WHERE appid=? AND objfrom=? AND objto=?");
			stc.setInt(1,appid);
			stc.setInt(2,fromnode);
			stc.setInt(3,tonode);
			ResultSet rsc = stc.executeQuery();
			int c=0;
			if (rsc.next())
			{
				c = rsc.getInt(1);
			}
			rsc.close();
			stc.close();
			if (c == 0) {
				// This flow does not exist
				PreparedStatement st = getDBConnection().prepareStatement("INSERT INTO dm.dm_applicationcomponentflows(appid,objfrom,objto) VALUES(?,?,?)");
				st.setInt(1,appid);
				if (fromnode>0) {
					st.setInt(2,fromnode);
				} else {
					st.setNull(2, Types.INTEGER);
				}
				if (tonode>0) {
					st.setInt(3,tonode);
				} else {
					st.setNull(3, Types.INTEGER);
				}
				st.execute();
				st.close();
				getDBConnection().commit();
			}
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create Link between components for application in Database");
	}
	
	public void applicationComponentDeleteLink(int appid,int fromnode,int tonode)
	{
		try
		{
			System.out.println("Deleting link between components for application");
			System.out.println("appid="+appid+" fromnode="+fromnode+" tonode (compid)="+tonode);
			PreparedStatement st = getDBConnection().prepareStatement("DELETE FROM dm.dm_applicationcomponentflows WHERE appid=? AND objto=?");
			st.setInt(1,appid);
			st.setInt(2,tonode);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create Link between components for application in Database");
	}
	
	public Component applicationReplaceComponent(int appid,int oldcompid,int newcompid, boolean isRelease)
	{
		try
		{
			System.out.println("Replacing component "+oldcompid+" with "+newcompid+" for application "+appid);
			PreparedStatement st = null;
			if (isRelease)
			 st = getDBConnection().prepareStatement("UPDATE dm.dm_applicationcomponent SET childappid=? WHERE appid=? AND childappid=?");
			else
			 st = getDBConnection().prepareStatement("UPDATE dm.dm_applicationcomponent SET compid=? WHERE appid=? AND compid=?");
			st.setInt(1,newcompid);
			st.setInt(2,appid);
			st.setInt(3,oldcompid);
			st.execute();
			st.close();
			PreparedStatement st2 = getDBConnection().prepareStatement("UPDATE dm.dm_applicationcomponentflows SET objto=? WHERE appid=? AND objto=?");
			st2.setInt(1,newcompid);
			st2.setInt(2,appid);
			st2.setInt(3,oldcompid);
			st2.execute();
			st2.close();
			PreparedStatement st3 = getDBConnection().prepareStatement("UPDATE dm.dm_applicationcomponentflows SET objfrom=? WHERE appid=? AND objfrom=?");
			st3.setInt(1,newcompid);
			st3.setInt(2,appid);
			st3.setInt(3,oldcompid);
			st3.execute();
			st3.close();
			getDBConnection().commit();
			Component comp = getComponent(newcompid,true);
			return comp;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Create Link between components for application in Database");
	}
	
	public void DeleteConnector(int envid,int fromnode,int tonode,int fromside,int toside)
	{
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("DELETE FROM dm.dm_server_connections WHERE envid=? AND serverfrom=? AND serverfromedge=? AND serverto=? AND servertoedge=?");
			st.setInt(1,envid);
			st.setInt(2,fromnode);
			st.setInt(3,fromside);
			st.setInt(4,tonode);
			st.setInt(5,toside);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Delete Connector from database");
	}
	
	public void applicationDeleteVersionDependency(int appid,int fromnode,int tonode)
	{
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_application SET predecessorid=? WHERE id=? AND parentid=? AND predecessorid=?");
			st.setInt(1,appid);
			st.setInt(2,tonode);
			st.setInt(3,appid);
			st.setInt(4,fromnode);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Delete Version Dependency from database");
	}
	
	public void componentDeleteVersionDependency(int compid,int fromnode,int tonode)
	{
		try
		{
			// Delete the old component item list
			PreparedStatement s1 = getDBConnection().prepareStatement("DELETE FROM dm.dm_compitemprops WHERE compitemid in (SELECT id FROM dm.dm_componentitem WHERE compid=?)");
			s1.setInt(1,tonode);
			s1.execute();
			s1.close();
			PreparedStatement s2 = getDBConnection().prepareStatement("DELETE FROM dm.dm_componentitem WHERE compid=?");
			s2.setInt(1,tonode);
			s2.execute();
			s2.close();
			// Update the predecessor to create the dependency
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_component SET predecessorid=? WHERE id=? AND parentid=? AND predecessorid=?");
			st.setInt(1,compid);
			st.setInt(2,tonode);
			st.setInt(3,compid);
			st.setInt(4,fromnode);
			st.execute();
			st.close();
//			AddComponentItems(timeNow(),tonode,compid);
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Delete Component Version Dependency from database");
	}
	
	public void componentItemDeleteLink(int compid,int fromnode,int tonode)
	{
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_componentitem SET predecessorid=null WHERE id=? AND compid=? AND predecessorid=?");
			st.setInt(1,tonode);
			st.setInt(2,compid);
			st.setInt(3,fromnode);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Delete Component Item Link from database");
	}
	
	
	public void applicationModifyVersion(int appid,String Name,String Summary)
	{
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_application SET name=?, summary=? WHERE id=?");
			st.setString(1,Name);
			st.setString(2,Summary);
			st.setInt(3,appid);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Modify Version in database");
	}
	
	public void componentModifyVersion(int compid,String Name,String Summary)
	{
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_component SET name=?, summary=? WHERE id=?");
			st.setString(1,Name);
			st.setString(2,Summary);
			st.setInt(3,compid);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Modify Component Version in database");
	}
	
	public void componentItemModifyItem(int ccid,String Name,String Summary)
	{
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_componentitem SET name=?, summary=? WHERE id=?");
			st.setString(1,Name);
			st.setString(2,Summary);
			st.setInt(3,ccid);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Modify Component Item in database");
	}
	
	public void serverSaveSummary(int servid,String Name,String Summary)
	{
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_server SET name=?, summary=? WHERE id=?");
			st.setString(1,Name);
			st.setString(2,Summary);
			st.setInt(3,servid);
			st.execute();
			st.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			rollback();
			ex.printStackTrace();
		}
		throw new RuntimeException("Unable to Modify Server in database");
	}
	
	public List<Server> getServers(int envid,boolean unallocated)
	{
		String sql;
		
		if (envid >= 0)
		{
		 if (unallocated)
		 {
		 	// Only list servers which have got no entry in dm_serversinenv
		 	sql = "select a.id,a.name,a.summary from dm.dm_server a where a.domainid in ("+m_domainlist+") and not exists (select 'Y' from dm.dm_serversinenv b where b.serverid = a.id)";
		 }
		 else
		 {
		 	// List all servers apart from those which are already allocated to the specified environment
		 	sql = "select a.id,a.name,a.summary from dm.dm_server a where a.domainid in ("+m_domainlist+") and a.id not in (select b.serverid from dm.dm_serversinenv b where b.envid = "+envid+")";
		 }
		}
		else
  {
    // Only list servers which have got no entry in dm_serversinenv
    sql = "select a.id,a.name,a.summary from dm.dm_server a where a.domainid in ("+m_domainlist+") order by 2";
  }
		
		try
		{
			List <Server> ret = new ArrayList<Server>();
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				Server s = new Server();
				s.setId(rs.getInt(1));
				s.setName(rs.getString(2));
				s.setSummary(rs.getString(3));
				ret.add(s);
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve Servers from database");
	}
	
	public List <ServerLink> GetServerLinks(int envid)
	{
		try
		{
			List <ServerLink> ret = new ArrayList<ServerLink>();
			PreparedStatement stmt = getDBConnection().prepareStatement("SELECT serverfrom,serverfromedge,serverto,servertoedge,label,style FROM dm.dm_server_connections WHERE envid=?");
			stmt.setInt(1,envid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				ServerLink sl = new ServerLink(rs.getInt(1),rs.getInt(3),rs.getInt(2),rs.getInt(4));
				sl.setLabel(rs.getString(5));
				sl.setStyle(rs.getString(6));
				ret.add(sl);
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve Server Links from database");
	}
	
	public List<Component> getComponentsInEnvironment(int envid)
	{
		try
		{
			List <Component> ret = new ArrayList<Component>();
			PreparedStatement stmt = getDBConnection().prepareStatement("SELECT a.id,a.name,a.summary FROM dm.dm_component a," +
															"dm.dm_compsallowedonserv b,	"	+
															"dm.dm_serversinenv c	"	+
															"WHERE a.id = b.compid	"	+
															"AND b.serverid=c.serverid	" +
															"AND c.envid=?"
															);
			stmt.setInt(1,envid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				Component c = new Component();
				c.setId(rs.getInt(1));
				c.setName(rs.getString(2));
				c.setSummary(rs.getString(3));
				ret.add(c);
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve Components for environment from database");
	}
	
	public List <Server> GetServersWithComponents(int envid,int compid)
	{
		try
		{
			List <Server> ret = new ArrayList<Server>();
			PreparedStatement stmt = getDBConnection().prepareStatement("SELECT a.id,a.name,a.summary FROM dm.dm_server a," +
															"dm.dm_compsallowedonserv b,	"	+
															"dm.dm_serversinenv c	"	+
															"WHERE a.id = b.serverid	"	+
															"AND b.compid=?	" +
															"AND b.serverid=c.serverid	" +
															"AND c.envid=?"
															);
			stmt.setInt(1,compid);
			stmt.setInt(2,envid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				Server s = new Server();
				s.setId(rs.getInt(1));
				s.setName(rs.getString(2));
				s.setSummary(rs.getString(3));
				ret.add(s);
			}
			rs.close();
			return ret;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve Servers containing components from database");
	}
	
 public List <Server> GetServersWithComponentsDeployed(int envid,int compid)
 {
  try
  {
   List <Server> ret = new ArrayList<Server>();
   PreparedStatement stmt = getDBConnection().prepareStatement("SELECT a.id,a.name,a.summary FROM dm.dm_server a," +
               "dm.dm_compsonserv b, " +
               "dm.dm_serversinenv c " +
               "WHERE a.id = b.serverid " +
               "AND b.compid=? " +
               "AND b.serverid=c.serverid " +
               "AND c.envid=?"
               );
   stmt.setInt(1,compid);
   stmt.setInt(2,envid);
   ResultSet rs = stmt.executeQuery();
   while (rs.next())
   {
    Server s = new Server();
    s.setId(rs.getInt(1));
    s.setName(rs.getString(2));
    s.setSummary(rs.getString(3));
    ret.add(s);
   }
   rs.close();
   return ret;
  }
  catch(SQLException e)
  {
   e.printStackTrace();
   rollback();
  }
  throw new RuntimeException("Unable to retrieve Servers containing components from database");
 }
 
	public void EnterEditMode(int actionid)
	{
		try
		{
			// Entering edit mode
			//
			// Need to check if anyone else is editing this
			//
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT count(*) FROM dm.dm_actionedit WHERE actionid="+actionid);
			rs.next();
			int c = rs.getInt(1);
			rs.close();
			if (c>0)
			{
				// already being edited
			}
			else
			{
				//                                                                                      FOR NOW
				//                                                                                     ---------
				st.execute("INSERT INTO dm.dm_actionedit(actionid,editid,userid) VALUES ("+actionid+","+actionid+","+GetUserID()+")");
			}
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Add Flow to database");
	}
	
	public void LeaveEditMode(int actionid)
	{
		try
		{
			// Leaving edit mode
			//
			// Need to do all the copy back at this point
			//
			Statement st = getDBConnection().createStatement();
			st.execute("DELETE FROM dm.dm_actionedit WHERE actionid="+actionid+" AND userid="+GetUserID());		
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Add Flow to database");
	}
	
	
	public static boolean getBoolean(ResultSet rs, int col, boolean defaultValue)
		throws SQLException
	{
		String val = rs.getString(col);
		return (!rs.wasNull() && (val != null)) ? val.equalsIgnoreCase("Y") : defaultValue;
	}
	
	public static int getInteger(ResultSet rs, int col, int defaultValue)
		throws SQLException
	{
		int val = rs.getInt(col);
		return !rs.wasNull() ? val : defaultValue;
	}
	
	public static long getLong(ResultSet rs, int col, int defaultValue)
			throws SQLException
		{
			long val = rs.getLong(col);
			return !rs.wasNull() ? val : defaultValue;
		}
	
	public void setIntegerIfGreaterThanZero(PreparedStatement stmt, int col, int value)
		throws SQLException
	{
		if(value > 0) {
			stmt.setInt(col, value);
		} else {
			stmt.setNull(col, Types.INTEGER);
		}
	}
	
	public static String getString(ResultSet rs, int col, String defaultValue)
		throws SQLException
	{
		String val = rs.getString(col);
		return (!rs.wasNull() && (val != null)) ? val : defaultValue;
	}
	
	public void getCreatorModifier(ResultSet rs, int startcol, DMObject obj)
			throws SQLException
	{
		int creatorid = getInteger(rs, startcol, 0);
		if(creatorid != 0) {
			obj.setCreator(new User(this, creatorid, rs.getString(startcol+1), rs.getString(startcol+2)));
		}
		obj.setCreated(getInteger(rs, startcol+3, 0));
		int modifierid = getInteger(rs, startcol+4, 0);
		if(modifierid != 0) {
			obj.setModifier(new User(this, modifierid, rs.getString(startcol+5), rs.getString(startcol+6)));
		}
		obj.setModified(getInteger(rs, startcol+7, 0));
	}

	private void getCreatorModifierOwner(ResultSet rs, int startcol, DMObject obj)
		throws SQLException
	{
		getCreatorModifier(rs, startcol, obj);
		int ownerid = getInteger(rs, startcol+8, 0);
		if(ownerid != 0) {
			obj.setOwner(new User(this, ownerid, rs.getString(startcol+9), rs.getString(startcol+10)));
		} else {
			int groupid = getInteger(rs, startcol+11, 0);
			if(groupid != 0) {
				obj.setOwner(new UserGroup(this, groupid, rs.getString(startcol+12)));
			}
		}
	}
	
	public void getStatus(ResultSet rs, int col, DMObject obj)
		throws SQLException
	{
		String status = rs.getString(col);
		obj.setDeleted(status.equalsIgnoreCase("D"));
		obj.setUnconfigured(status.equalsIgnoreCase("U"));
	}
	
	private void getPreAndPostActions(ResultSet rs, int col, IPrePostAction obj)
			throws SQLException
	{
		int preactionid = getInteger(rs, col, 0);
		if (preactionid != 0) {
			Action preaction = getAction(preactionid,false);
			obj.setPreAction(preaction);
		}
		int postactionid = getInteger(rs, col+3, 0);
		if (postactionid != 0) {
			Action postaction = getAction(postactionid,false);
			obj.setPostAction(postaction);
		}
	}
	
	public String formatDateToUserLocale(int when)
	{
		// TODO: Lookup User's locale and format string
		System.out.println("df = "+m_datefmt+" "+m_timefmt);
		SimpleDateFormat sdf = new SimpleDateFormat(m_datefmt + " " + m_timefmt);
		return sdf.format(new java.util.Date(1000 * (long) when));
	}
	
	public Category createNewCategory(String name)
	{
		int count = -1;
		try {
			String csql = "SELECT count(*) FROM dm.dm_category WHERE name=?";
			PreparedStatement st1 = getDBConnection().prepareStatement(csql);
			st1.setString(1, name);
			ResultSet rs1 = st1.executeQuery();
			if(rs1.next()) {
				count = rs1.getInt(1);
				if(count == 0) {
					int newid = getID("category");
					if(newid > 0) {
						String isql="INSERT INTO dm.dm_category(id,name) VALUES(?,?)";
						PreparedStatement st2 = getDBConnection().prepareStatement(isql);
						st2.setInt(1,newid);
						st2.setString(2,name);
						st2.execute();
						st2.close();
						getDBConnection().commit();
						return new Category(newid, name);
					}
				}
			}
			rs1.close();
			st1.close();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}

		if(count > 0) {
			throw new RuntimeException("Category name already exists");
		}
		throw new RuntimeException("Failed to create category");
	}

 public Engine createEngine(int domainid, String name)
 {
  int count = -1;
  try {
   String csql = "SELECT count(*) FROM dm.dm_engine WHERE name=?";
   PreparedStatement st1 = getDBConnection().prepareStatement(csql);
   st1.setString(1, name);
   ResultSet rs1 = st1.executeQuery();
   if(rs1.next()) {
    count = rs1.getInt(1);
    if(count == 0) {
     int newid = getID("engine");
     if(newid > 0) {
      String isql="INSERT INTO dm.dm_engine(id,domainid,name,status) VALUES(?,?,?,'N')";
      PreparedStatement st2 = getDBConnection().prepareStatement(isql);
      st2.setInt(1,newid);
      st2.setInt(2,domainid);
      st2.setString(3,name);
      st2.execute();
      st2.close();
      getDBConnection().commit();
      return new Engine(this, newid, name);
     }
    }
   }
   rs1.close();
   st1.close();
  } catch(SQLException e) {
   e.printStackTrace();
   rollback();
  }

  if(count > 0) {
   throw new RuntimeException("Engine name already exists");
  }
  throw new RuntimeException("Failed to create engine");
 }

 
	public Category getCategory(int id)
	{
		if(id == 0) {
			return Category.NO_CATEGORY;
		}
		
		try
		{
			PreparedStatement st = getDBConnection().prepareStatement("SELECT c.id, c.name FROM dm.dm_category c WHERE c.id = ?");
			st.setInt(1, id);
			ResultSet rs = st.executeQuery();
			Category ret = null;
			if(rs.next()) {
				ret = new Category(rs.getInt(1),rs.getString(2));
			}
			rs.close();
			st.close();
			return ret;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Get Category "+id+" from Database");
	}
	
	public List<Category> GetCategories()
	{
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT id,name FROM dm.dm_category ORDER BY name");
			List<Category> res = new ArrayList<Category>();
			while (rs.next())
			{
				Category c = new Category(rs.getInt(1),rs.getString(2));
				res.add(c);
			}
			rs.close();
			st.close();
			return res;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Get Categories from Database");
	}
	
	public List<TreeObject> GetCategoriesAsTree()
 {
  try
  {
   Statement st = getDBConnection().createStatement();
   ResultSet rs = st.executeQuery("SELECT id,name FROM dm.dm_category ORDER BY name");
   List<TreeObject> res = new ArrayList<TreeObject>();
   while (rs.next())
   {
    TreeObject c = new TreeObject(rs.getInt(1),rs.getString(2));
    c.SetObjectType(ObjectType.ACTION_CATEGORY);
    res.add(c);
   }
   rs.close();
   st.close();
   return res;
  }
  catch (SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  throw new RuntimeException("Unable to Get Categories from Database");
 }
	
	public List<Category> GetFragmentsAndCategories()
	{
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT id,name FROM dm.dm_category ORDER BY name");
			List<Category> res = new ArrayList<Category>();
			while (rs.next())
			{
				Category c = new Category(rs.getInt(1),rs.getString(2));
				Statement st2 = getDBConnection().createStatement();
				ResultSet rs2 = st2.executeQuery("SELECT a.id,a.name,a.summary,a.exitpoints,a.drilldown, a.actionid FROM dm.dm_fragments a, dm.dm_fragment_categories b WHERE a.id = b.id and b.categoryid="+ c.getId() +" ORDER BY a.id");
				List<Fragment> Fragments = new ArrayList<Fragment>();
				while (rs2.next())
				{
					// id name summary exitpoints
					Fragments.add(new Fragment(rs2.getInt(1),rs2.getString(2),rs2.getString(3),rs2.getInt(4),rs2.getString(5),rs2.getInt(6)));
				}
				rs2.close();
				st2.close();
				c.setFragments(Fragments);
				res.add(c);
			}
			rs.close();
			st.close();
			return res;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Get Fragments and Categories from Database");
	}
	
	public int getMaxWindowID(int actionid)
	{
		try
		{
			int res=0;
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT max(windowid) FROM dm.dm_actionfrags WHERE actionid="+actionid);
			if (rs.next())
			{
				res = rs.getInt(1);
			} else {
				res = 0;
			}
			rs.close();
			st.close();
			return res;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Get Max Window ID from Database");
	}
	
	public List<FragmentDetails> GetParentFragments(int actionid,int windowid)
	{
		List<FragmentDetails> res = new ArrayList<FragmentDetails>();
		FragmentDetails fd = getFragmentDetails(actionid,windowid);
		if (fd.getFragmentName().length()==0) fd.setFragmentName(fd.getTypeName());
		res.add(fd);
		while (fd.getParentWindow()>0) {
			fd = getFragmentDetails(actionid,fd.getParentWindow());
			if (fd.getFragmentName().length()==0) fd.setFragmentName(fd.getTypeName());
			res.add(fd);
		}
		List<FragmentDetails> invertedList = new ArrayList<FragmentDetails>();
	    for (int i = res.size() - 1; i >= 0; i--) {
	        invertedList.add(res.get(i));
	    }
	    return invertedList;
	}
	
	public List<Component> getComponentsInDomain(boolean IncludeVersions)
	{
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs;
			if (IncludeVersions) {
				rs = st.executeQuery("SELECT id,name,summary,parentid FROM dm.dm_component WHERE domainid in ("+m_domainlist+") and status = 'N' ORDER BY name,parentid");
			} else {
				rs = st.executeQuery("SELECT id,name,summary,parentid FROM dm.dm_component WHERE domainid in ("+m_domainlist+") AND parentid IS NULL  and status = 'N' ORDER BY name,parentid");
			}
			List<Component> res = new ArrayList<Component>();
			while (rs.next())
			{
				int parentid = rs.getInt(4);
				if (rs.wasNull()) {
					// Parent Component
					res.add(new Component(this,rs.getInt(1),rs.getString(2),rs.getString(3)));
				} else {
					// child (version) component - add these to the appropriate version
					for (Component c: res) {
						if (c.getId() == parentid) {
							// Found it
							Component childversion = new Component(this,rs.getInt(1),rs.getString(2),rs.getString(3));
							List<Component> vl = c.getVersions();
							if (vl == null) vl = new ArrayList<Component>();
							vl.add(childversion);
							c.setVersions(vl);
							break;
						}
					}
				}
			}
			rs.close();
			st.close();
			return res;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Get Components and Versions from Database");
	}
	
	public List<Application> getApplicationsInDomain(int domainid) {
		try
		{
			List<Application> res = new ArrayList<Application>();
			Statement st = getDBConnection().createStatement();
			ResultSet rs;
			rs = st.executeQuery("SELECT id FROM dm.dm_application WHERE domainid="+domainid+" AND status='N' ORDER BY name");
			while (rs.next())
			{
				Application app = getApplication(rs.getInt(1),true);
				res.add(app);
			}
			rs.close();
			st.close();
			return res;
		} catch(SQLException ex) {
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Get Applications in Domain from Database");
	}
	
	public List<Application> getApplicationsInDomain(boolean IncludeVersions, boolean IncludeReleases)
	{
		System.out.println("getApplicationsInDomain("+IncludeVersions+","+IncludeReleases+")");
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs;
			if (IncludeVersions) {
				rs = st.executeQuery("SELECT id,name,summary,parentid, isrelease FROM dm.dm_application WHERE domainid in ("+m_domainlist+") AND status='N' ORDER BY name");
			} else {
				rs = st.executeQuery("SELECT id,name,summary,parentid, isrelease FROM dm.dm_application WHERE domainid in ("+m_domainlist+") AND status='N' AND parentid IS NULL ORDER BY name,parentid");
			}
			List<Application> res = new ArrayList<Application>();
			while (rs.next())
			{
				String rel = getString(rs,5,"N");
				if (IncludeReleases || (!IncludeReleases && rel.equalsIgnoreCase("N"))) {
					int parentid = rs.getInt(4);
					if (rs.wasNull()) {
						// Parent Component
						Application a = new Application(this,rs.getInt(1),rs.getString(2));
						a.setSummary(rs.getString(3));
						a.setIsRelease(rel);
						res.add(a);
					} else {
						// child (version) application - add these to the appropriate version
						for (Application a: res) {
							if (a.getId() == parentid) {
								// Found it
								Application childversion = new Application(this,rs.getInt(1),rs.getString(2));
								childversion.setSummary(rs.getString(3));
								childversion.setIsRelease(rel);
								List<Application> vl = a.getVersions();
								if (vl == null) vl = new ArrayList<Application>();
								vl.add(childversion);
								a.setVersions(vl);
								break;
							}
						}
					}
				}
			}
			rs.close();
			st.close();
			return res;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Get Applications and Versions from Database");
	}
	
 public List<Application> getDeployableApplicationsInDomain(Domain domain, Domain parent_domain, boolean IncludeVersions, boolean IncludeReleases)
 {
  System.out.println("getApplicationsInDomain("+IncludeVersions+","+IncludeReleases+")");
  try
  {
   Statement st = getDBConnection().createStatement();
   ResultSet rs;
   String domlist = "";
   String domquery = "domainid in ("+m_domainlist+") AND ";
   
   ResultSet rs2 = st.executeQuery("select id, position from dm.dm_domain where domainid = " + parent_domain.getId() + " and position >= (select position from dm.dm_domain where id = " + domain.getId() + ") order by position asc");
   while (rs2.next())
   {
    domlist += "," + rs2.getInt(1);
   }
   rs2.close();
   st.close();
   
   if (domlist.length()>1) {
	   domquery = "domainid in ("+domlist.substring(1)+") AND ";
   }
   st = getDBConnection().createStatement();
   
   if (IncludeVersions) {
    rs = st.executeQuery("SELECT id,name,summary,parentid, isrelease FROM dm.dm_application WHERE "+domquery+" status='N' ORDER BY name");
   } else {
    rs = st.executeQuery("SELECT id,name,summary,parentid, isrelease FROM dm.dm_application WHERE "+domquery+" status='N' AND parentid IS NULL ORDER BY name,parentid");
   }
   List<Application> res = new ArrayList<Application>();
   while (rs.next())
   {
    String rel = getString(rs,5,"N");
    if (IncludeReleases || (!IncludeReleases && rel.equalsIgnoreCase("N"))) {
     int parentid = rs.getInt(4);

     if (!rs.wasNull())
     {
      int k = 0;
      Statement st3 = getDBConnection().createStatement();
      ResultSet rs3 = st3.executeQuery("select count(*) from dm.dm_application where id = " + parentid + " and status = 'N'");
      while (rs3.next())
      {
       k = rs3.getInt(1);
      }
      rs3.close();      
      st3.close();
      
      if (k > 0)
      {
       Application a = new Application(this,rs.getInt(1),rs.getString(2));
       a.setSummary(rs.getString(3));
       a.setIsRelease(rel);
       res.add(a);
      }
     }
     else
     {
      Application a = new Application(this,rs.getInt(1),rs.getString(2));
      a.setSummary(rs.getString(3));
      a.setIsRelease(rel);
      res.add(a);
     }
    }
   }
   rs.close();
   st.close();
   return res;
  }
  catch (SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  throw new RuntimeException("Unable to Get Applications and Versions from Database");
 }
 
	public List<Application> getChildApplicationVersions(Application app)
	{
		List<Application> ret = new ArrayList<Application>();
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT id FROM dm.dm_application WHERE domainid in ("+m_domainlist+") AND status='N' AND parentid="+app.getId()+" ORDER BY name");
			while (rs.next()) {
				Application ca = getApplication(rs.getInt(1),true);
				ret.add(ca);
			}
			rs.close();
			st.close();
			return ret;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to Get Child Applications from Database");
	}
	
	
	public List<Environment> getEnvironmentsInDomain()
	{
		try
		{
			Statement st = getDBConnection().createStatement();
			ResultSet rs = st.executeQuery(
				  "SELECT e.id, e.name, e.summary,e.domainid FROM dm.dm_environment e "
				+ "WHERE e.domainid in ("+m_domainlist+") AND status='N' ORDER BY e.domainid,e.id");
			List<Environment> res = new ArrayList<Environment>();
			while(rs.next())
			{
				Environment e = new Environment(this,rs.getInt(1),rs.getString(2));
				e.setSummary(rs.getString(3));
				e.setDomainId(rs.getInt(4));
				res.add(e);
			}
			rs.close();
			st.close();
			return res;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to get Environments from database");
	}
	
 public List<Environment> getEnvironmentsInDomain(Domain mydomain)
 {
  try
  {
   Statement st = getDBConnection().createStatement();
   ResultSet rs = st.executeQuery(
      "SELECT e.id, e.name, e.summary,e.domainid FROM dm.dm_environment e "
    + "WHERE e.domainid in ("+mydomain.getId()+") AND status='N' ORDER BY e.domainid,e.id");
   List<Environment> res = new ArrayList<Environment>();
   while(rs.next())
   {
    Environment e = new Environment(this,rs.getInt(1),rs.getString(2));
    e.setSummary(rs.getString(3));
    e.setDomainId(rs.getInt(4));
    res.add(e);
   }
   rs.close();
   st.close();
   return res;
  }
  catch (SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  throw new RuntimeException("Unable to get Environments from database");
 }

// TODO: Phil is this used? RHT 31/01/2014
//	int getBaseDomainForApplication(int appid)
//	{
//		//
//		// Given an application, returns the domain in which it currently resides. Given an
//		// application *version*, returns the domain of the "base" application. Used for
//		// Copy/Move tasks to prevent the application version being moved above the parent.
//		//
//		try
//		{
//			PreparedStatement ps = m_conn.prepareStatement(
//						"SELECT a.domainid,b.domainid	"
//					+	"FROM dm.dm_application a 		"
//					+	"LEFT OUTER JOIN dm.dm_application b ON a.parentid = b.id	"
//					+	"WHERE a.id=?");
//			ps.setInt(1,appid);
//			ResultSet rs = ps.executeQuery();
//			if (rs.next())
//			{
//				int domainid=rs.getInt(1);
//				int parentdomainid = rs.getInt(2);
//				int res = rs.wasNull()?domainid:parentdomainid;
//				rs.close();
//				ps.close();
//				return res;
//			}
//		}
//		catch (SQLException ex)
//		{
//			ex.printStackTrace();
//			rollback();
//		}
//		throw new RuntimeException("Unable to Get Base Domain of Application from Database");
//	}
	
	//
	// Task Stuff
	// ----------
	//
	public TaskApprove getTaskApprove(int tid)
	{
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement(
				"SELECT a.id, a.name, a.domainid, a.logoutput, a.subdomains, b.approvaldomain, a.successtemplateid, a.failuretemplateid, "
					+ " uc.id, uc.name, uc.realname, a.created, "
					+ " um.id, um.name, um.realname, a.modified, "
					+ " a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
					+ "FROM dm.dm_task	a	"
					+ "LEFT OUTER JOIN dm.dm_taskapprove b ON b.id = a.id "
					+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
					+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
					+ "WHERE a.id=?"
			);
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				System.out.println("Getting new TaskApprove object");
				TaskApprove ret = new TaskApprove(this,rs.getInt(1),rs.getString(2));	
				ret.setDomainId(rs.getInt(3));
				ret.setShowOutput(getBoolean(rs, 4, false));
				ret.setSubDomains(getBoolean(rs, 5, false));
				Domain appdomain = getDomain(rs.getInt(6));
				ret.setApprovalDomain(appdomain);
				
				int stid = getInteger(rs,7,0);
				int ftid = getInteger(rs,8,0);
				System.out.println("stid="+stid+" ftid="+ftid);
				NotifyTemplate nt = stid>0?getTemplate(stid):null;
				NotifyTemplate rt = ftid>0?getTemplate(ftid):null;
				ret.setSuccessTemplate(nt);
				ret.setFailureTemplate(rt);
				getCreatorModifier(rs, 9, ret);
				getPreAndPostActions(rs, 17, ret);
				return ret;
			}
			else
			{
				// No row found - error
				throw new RuntimeException("Unable to Get Task Approve from Database");
			}
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}
	
 public String getTaskAudit(String home, int serverid)
 {
  JSONObject obj = new JSONObject();
  obj.add("serverid", serverid);

    TaskAudit audit = new TaskAudit(this, home, serverid);
    audit.run();

  obj.add("showoutput", audit.getShowOutput());
  String ret = obj.getJSON();

  return ret;
 }

 public String getTaskAudit(String home, int envid, boolean isEnv)
 {
  JSONObject obj = new JSONObject();
  obj.add("envid", envid);
  String output = "";
  
  try
  {
   PreparedStatement st = getDBConnection().prepareStatement("SELECT a.serverid WHERE a.envid=?");
  
   st.setInt(1,envid);
   ResultSet rs = st.executeQuery();
   while (rs.next())
   {
    int serverid = rs.getInt(1);
    TaskAudit audit = new TaskAudit(this, home, serverid);
    audit.run();
    output += audit.getShowOutput() + " ";
   }
  }
  catch (SQLException ex)
  {
   ex.printStackTrace();
   rollback();
  }
  
  obj.add("showoutput", output);
  String ret = obj.getJSON();

  return ret;
 }
 
	public TaskMove getTaskMove(int tid)
	{
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement(
				"SELECT a.id, a.name, a.domainid, a.logoutput, a.subdomains, b.targetdomain, a.successtemplateid, a.failuretemplateid, "
					+ " uc.id, uc.name, uc.realname, a.created, "
					+ " um.id, um.name, um.realname, a.modified, "
					+ " a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
					+ "FROM dm.dm_task	a	"
					+ "LEFT OUTER JOIN dm.dm_taskmove b ON b.id = a.id "
					+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
					+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
					+ "WHERE a.id=?"
			);
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				TaskMove ret = new TaskMove(this,rs.getInt(1),rs.getString(2));
				ret.setDomainId(rs.getInt(3));
				ret.setShowOutput(getBoolean(rs, 4, false));
				ret.setSubDomains(getBoolean(rs, 5, false));
				Domain domain = getDomain(rs.getInt(6));
				ret.setTargetDomain(domain);
				int stid = getInteger(rs,7,0);
				int ftid = getInteger(rs,8,0);
				System.out.println("stid="+stid+" ftid="+ftid);
				NotifyTemplate nt = stid>0?getTemplate(stid):null;
				NotifyTemplate rt = ftid>0?getTemplate(ftid):null;
				ret.setSuccessTemplate(nt);
				ret.setFailureTemplate(rt);
				getCreatorModifier(rs, 9, ret);
				getPreAndPostActions(rs, 17, ret);
				return ret;
			}
			else
			{
				// No row found - error
				throw new RuntimeException("Unable to Get Task Move from Database");
			}
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}
	
	public TaskRemove getTaskRemove(int tid)
	{
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement(
				"SELECT a.id, a.name, a.domainid, a.logoutput, a.subdomains, "
					+ " uc.id, uc.name, uc.realname, a.created, "
					+ " um.id, um.name, um.realname, a.modified, "
					+ " a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
					+ "FROM dm.dm_task		a	"
					+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
					+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
					+ "WHERE a.id=?"
			);
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				TaskRemove ret = new TaskRemove(this,rs.getInt(1),rs.getString(2));
				ret.setDomainId(rs.getInt(3));
				ret.setShowOutput(getBoolean(rs, 4, false));
				ret.setSubDomains(getBoolean(rs, 5, false));
				getCreatorModifier(rs, 6, ret);
				getPreAndPostActions(rs, 14, ret);
				return ret;
			}
			else
			{
				// No row found - error
				throw new RuntimeException("Unable to Get Task Remove from Database");
			}
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}
	
	public TaskDeploy getTaskDeploy(int tid)
	{
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement(
				"SELECT a.id, a.name, a.domainid, a.logoutput,	"
					+ " uc.id, uc.name, uc.realname, a.created, "
					+ " um.id, um.name, um.realname, a.modified, "
					+ " a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
					+ "FROM dm.dm_task	a	"
					+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
					+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
					+ "WHERE a.id=?"
			);
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				TaskDeploy ret = new TaskDeploy(this,rs.getInt(1),rs.getString(2));
				ret.setDomainId(rs.getInt(3));
				ret.setShowOutput(getBoolean(rs, 4, false));
				getCreatorModifier(rs, 5, ret);
				getPreAndPostActions(rs, 13, ret);
				return ret;
			}
			else
			{
				// No row found - error
				throw new RuntimeException("Unable to Get Task Deploy from Database");
			}
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}
	
	public TaskAction getTaskAction(int tid)
	{
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement(
				"SELECT a.id, a.name, a.domainid, a.logoutput, a.subdomains, b.actionid, a.successtemplateid,	a.failuretemplateid, "
					+ " uc.id, uc.name, uc.realname, a.created, "
					+ " um.id, um.name, um.realname, a.modified, "
					+ " a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
					+ "FROM dm.dm_task	a	"
					+ "LEFT OUTER JOIN dm.dm_taskaction b ON b.id = a.id "
					+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
					+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
					+ "WHERE a.id=?"
			);
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				TaskAction ret = new TaskAction(this,rs.getInt(1),rs.getString(2));
				ret.setDomainId(rs.getInt(3));
				ret.setShowOutput(getBoolean(rs, 4, false));
				ret.setSubDomains(getBoolean(rs, 5, false));
				int actionid = getInteger(rs,6,0);
				if (actionid > 0) {
					Action action = getAction(actionid, true);
					ret.setAction(action);
				}
				int stid = getInteger(rs,7,0);
				int ftid = getInteger(rs,8,0);
				NotifyTemplate nt = stid>0?getTemplate(stid):null;
				NotifyTemplate ft = ftid>0?getTemplate(ftid):null;
				ret.setSuccessTemplate(nt);
				ret.setFailureTemplate(ft);
				getCreatorModifier(rs, 9, ret);
				getPreAndPostActions(rs, 17, ret);
				return ret;
			}
			else
			{
				// No row found - error
				throw new RuntimeException("Unable to Get Task Action from Database");
			}
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}
	
	public TaskRequest getTaskRequest(int tid)
	{
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement(
				"SELECT a.id, a.name, a.domainid, a.logoutput, a.subdomains, b.linkedtaskid, a.successtemplateid,	"
					+ " uc.id, uc.name, uc.realname, a.created, "
					+ " um.id, um.name, um.realname, a.modified, "
					+ " a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
					+ "FROM dm.dm_task	a	"
					+ "LEFT OUTER JOIN dm.dm_taskrequest b ON b.id = a.id "
					+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
					+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
					+ "WHERE a.id=?"
			);
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				TaskRequest ret = new TaskRequest(this,rs.getInt(1),rs.getString(2));
				ret.setDomainId(rs.getInt(3));
				ret.setShowOutput(getBoolean(rs, 4, false));
				ret.setSubDomains(getBoolean(rs, 5, false));
				int linkedtask = rs.getInt(6);
				if (!rs.wasNull() && (linkedtask > 0)) {
					Task linkedTask = getTask(linkedtask,false);
					ret.setLinkedTask(linkedTask);
				}
				int stid = getInteger(rs,7,0);
				NotifyTemplate nt = stid>0?getTemplate(stid):null;
				ret.setSuccessTemplate(nt);
				getCreatorModifier(rs, 8, ret);
				getPreAndPostActions(rs, 16, ret);
				return ret;
			}
			else
			{
				// No row found - error
				throw new RuntimeException("Unable to Get Task Request from Database");
			}
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}

	
	public TaskCreateVersion getTaskCreateVersion(int tid)
	{
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement(
				"SELECT a.id, a.name, a.domainid, a.logoutput, a.subdomains, b.targetdomain,	"
					+ " uc.id, uc.name, uc.realname, a.created, "
					+ " um.id, um.name, um.realname, a.modified, "
					+ " a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
					+ "FROM dm.dm_task	a	"
					+ "LEFT OUTER JOIN dm.dm_taskcreateversion b ON b.id = a.id "
					+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
					+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
					+ "WHERE a.id=?"
			);
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				TaskCreateVersion ret = new TaskCreateVersion(this,rs.getInt(1),rs.getString(2));
				ret.setDomainId(rs.getInt(3));
				ret.setShowOutput(getBoolean(rs, 4, false));
				ret.setSubDomains(getBoolean(rs, 5, false));
				Domain domain = getDomain(rs.getInt(6));
				ret.setTargetDomain(domain);
				getCreatorModifier(rs, 7, ret);
				getPreAndPostActions(rs, 15, ret);
				return ret;
			}
			else
			{
				// No row found - error
				throw new RuntimeException("Unable to Get Task Create Version from Database");
			}
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}

	
	public TaskUserDefined getTaskUserDefined(int tid)
	{
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement(
				"SELECT a.id, a.name, a.domainid, a.logoutput, "
					+ " uc.id, uc.name, uc.realname, a.created, "
					+ " um.id, um.name, um.realname, a.modified, "
					+ " a1.id, a1.name, a1.domainid, a2.id, a2.name, a2.domainid "
					+ "FROM dm.dm_task	a	"
					+ "LEFT OUTER JOIN dm.dm_user uc ON a.creatorid = uc.id "		// creator
					+ "LEFT OUTER JOIN dm.dm_user um ON a.modifierid = um.id "		// modifier
					+ "LEFT OUTER JOIN dm.dm_action a1 ON a.preactionid = a1.id "	// pre-action
					+ "LEFT OUTER JOIN dm.dm_action a2 ON a.postactionid = a2.id "	// post-action
					+ "WHERE a.id=?"
			);
			st.setInt(1,tid);
			ResultSet rs = st.executeQuery();
			if (rs.next())
			{
				TaskUserDefined ret = new TaskUserDefined(this,rs.getInt(1),rs.getString(2));
				ret.setDomainId(rs.getInt(3));
				ret.setShowOutput(getBoolean(rs, 4, false));
				getCreatorModifier(rs, 5, ret);
				getPreAndPostActions(rs, 13, ret);
				return ret;
			}
			else
			{
				// No row found - error
				throw new RuntimeException("Unable to Get Task User Defined from Database");
			}
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return null;
	}

	
	public boolean updateTaskCopy(int taskid,int tgtdomainid)
	{
		System.out.println("so.updateTaskCopy, taskid="+taskid+" tgtdomainid="+tgtdomainid);
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_taskcopy SET targetdomain=? WHERE id=?");
			st.setInt(1,tgtdomainid);
			st.setInt(2,taskid);
			st.execute();
			if (st.getUpdateCount()==0)
			{
				PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_taskcopy(id,targetdomain) VALUES(?,?)");
				st2.setInt(1,taskid);
				st2.setInt(2,tgtdomainid);
				st2.execute();
				st2.close();
			}
			st.close();
			getDBConnection().commit();
			return true;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateTaskMove(int taskid,int tgtdomainid)
	{
		System.out.println("so.updateTaskMove, taskid="+taskid+" tgtdomainid="+tgtdomainid);
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_taskmove SET targetdomain=? WHERE id=?");
			st.setInt(1,tgtdomainid);
			st.setInt(2,taskid);
			st.execute();
			if (st.getUpdateCount()==0)
			{
				PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_taskmove(id,targetdomain) VALUES(?,?)");
				st2.setInt(1,taskid);
				st2.setInt(2,tgtdomainid);
				st2.execute();
				st2.close();
			}
			st.close();
			getDBConnection().commit();
			return true;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateTaskApprove(int taskid,int appdomainid)
	{
		System.out.println("so.updateTaskApprove, taskid="+taskid+" tgtdomainid="+appdomainid);
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_taskapprove SET approvaldomain=? WHERE id=?");
			st.setInt(1,appdomainid);
			st.setInt(2,taskid);
			st.execute();
			if (st.getUpdateCount()==0)
			{
				PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_taskapprove(id,approvaldomain) VALUES(?,?)");
				st2.setInt(1,taskid);
				st2.setInt(2,appdomainid);
				st2.execute();
				st2.close();
			}
			st.close();
			getDBConnection().commit();
			return true;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateTaskRequest(int taskid,int linkedtaskid)
	{
		System.out.println("so.updateTaskRequest, taskid="+taskid+" linkedtaskid="+linkedtaskid);
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_taskrequest SET linkedtaskid=? WHERE id=?");
			st.setInt(1,linkedtaskid);
			st.setInt(2,taskid);
			st.execute();
			if (st.getUpdateCount()==0)
			{
				PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_taskrequest(id,linkedtaskid) VALUES(?,?)");
				st2.setInt(1,taskid);
				st2.setInt(2,linkedtaskid);
				st2.execute();
				st2.close();
			}
			st.close();
			getDBConnection().commit();
			return true;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateTaskAction(int taskid,int actionid)
	{
		System.out.println("so.updateTaskAction, taskid="+taskid+" actionid="+actionid);
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_taskaction SET actionid=? WHERE id=?");
			st.setInt(1,actionid);
			st.setInt(2,taskid);
			st.execute();
			if (st.getUpdateCount()==0)
			{
				PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_taskaction(id,actionid) VALUES(?,?)");
				st2.setInt(1,taskid);
				st2.setInt(2,actionid);
				st2.execute();
				st2.close();
			}
			st.close();
			getDBConnection().commit();
			return true;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateTaskCreateVersion(int taskid,int tgtdomainid)
	{
		System.out.println("so.updateTaskCreateVersion, taskid="+taskid+" tgtdomainid="+tgtdomainid);
		try
		{			
			PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_taskcreateversion SET targetdomain=? WHERE id=?");
			st.setInt(1,tgtdomainid);
			st.setInt(2,taskid);
			st.execute();
			if (st.getUpdateCount()==0)
			{
				PreparedStatement st2 = getDBConnection().prepareStatement("INSERT INTO dm.dm_taskcreateversion(id,targetdomain) VALUES(?,?)");
				st2.setInt(1,taskid);
				st2.setInt(2,tgtdomainid);
				st2.execute();
				st2.close();
			}
			st.close();
			getDBConnection().commit();
			return true;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateBranchLabel(int id,String objtype, String BranchName)
	{
		System.out.println("so.updateBranchLabel, objtype="+objtype+" appverid="+id+" BranchName="+BranchName);
		String tabname=(objtype.equalsIgnoreCase("cv"))?"dm_component":"dm_application";
		try
		{
			if (BranchName == null || BranchName.length()==0) {
				PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm."+tabname+" SET branch=null WHERE id=?");
				st.setInt(1,id);
				st.execute();
				st.close();
			} else {
				PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm."+tabname+" SET branch=? WHERE id=?");
				st.setString(1,BranchName);
				st.setInt(2,id);
				st.execute();
				st.close();
			}
			getDBConnection().commit();
			return true;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean updateServerLabel(int envid,int fromid,int toid,int fromside,int toside,String LabelName)
	{
		System.out.println("so.updateBranchLabel, fromid="+fromid+" toid="+toid+" fromside="+fromside+" toside="+toside+" LabelName="+LabelName);
		try
		{
			if (LabelName == null || LabelName.length()==0) {
				PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_server_connections SET label=null WHERE envid=? AND serverfrom=? AND serverto=? AND serverfromedge=? AND servertoedge=?");
				st.setInt(1,envid);
				st.setInt(2,fromid);
				st.setInt(3,toid);
				st.setInt(4,fromside);
				st.setInt(5,toside);
				st.execute();
				st.close();
			} else {
				PreparedStatement st = getDBConnection().prepareStatement("UPDATE dm.dm_server_connections SET label=? WHERE envid=? AND serverfrom=? AND serverto=? AND serverfromedge=? AND servertoedge=?");
				st.setString(1,LabelName);
				st.setInt(2,envid);
				st.setInt(3,fromid);
				st.setInt(4,toid);
				st.setInt(5,fromside);
				st.setInt(6,toside);
				st.execute();
				st.close();
			}
			getDBConnection().commit();
			return true;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
//	public boolean copyApplication(Application application,Domain domain)
//	{
//		System.out.println("in dmsesssion, copy application id="+application.getId()+" domain.id="+domain.getId());
//		try
//		{			
//			//
//			// This is a move strictly speaking. At the moment an application version can only exist in one domain
//			// at a time. Need to change this...
//			//
//			PreparedStatement st = m_conn.prepareStatement("UPDATE dm.dm_application SET domainid=? WHERE id=?");
//			st.setInt(1,domain.getId());
//			st.setInt(2,application.getId());
//			st.execute();
//			st.close();
//			m_conn.commit();
//			return true;
//		}
//		catch (SQLException ex)
//		{
//			ex.printStackTrace();
//			rollback();
//		}
//		return false;
//	}
	
	//
	// Access Control
	//
	public boolean AddAccess(DMObject obj,String t,int gid,String st,boolean DomainInheritance)
	{
		// domaininherit no longer separate table - just use domainaccess
		// String TableName=DomainInheritance?"dm_domaininherit":obj.getDatabaseTable()+"access";
		String TableName=DomainInheritance?"dm_domainaccess":obj.getDatabaseTable()+"access";
		String KeyCol=obj.getForeignKey();
		String AccessCol="";
		int ycol=0;
		boolean added=false;
		
		String yname=null;
		
		if (t.equalsIgnoreCase("va")) {AccessCol="viewaccess"; ycol=3; yname="View Access";}			// View Access		(all objects)
		else
		if (t.equalsIgnoreCase("ua")) {AccessCol="updateaccess"; ycol=4; yname="Change Access";}		// Update Access	(all objects)
		else
		if (t.equalsIgnoreCase("ra")) {AccessCol="readaccess"; ycol=5; yname=obj.getReadTitle();}		// Read Access		(some objects)
		else
		if (t.equalsIgnoreCase("wa")) {AccessCol="writeaccess"; ycol=6; yname=obj.getWriteTitle();}		// Write Access		(some objects)
		else
		if (t.equalsIgnoreCase("vx")) {AccessCol="viewaccess"; ycol=3; yname="View Access";}			// View Access		(for inheritance)
		else
		if (t.equalsIgnoreCase("ux")) {AccessCol="updateaccess"; ycol=4; yname="Change Access";}		// Update Access	(for inheritance)
		else
		if (t.equalsIgnoreCase("rx")) {AccessCol="readaccess"; ycol=5; yname=obj.getReadTitle();}		// Read Access		(for inheritance)
		else
		if (t.equalsIgnoreCase("wx")) {AccessCol="writeaccess"; ycol=6; yname=obj.getWriteTitle();}		// Write Access		(for inheritance)

		int id=obj.getId();
		
		System.out.println("Adding Group to object objtype="+obj.getObjectTypeAsInt()+" objid="+id+" t="+t+" gid="+gid);
		
		String sql="SELECT count(*) FROM dm."+TableName+" WHERE "+KeyCol+"="+id+" AND usrgrpid=?";
		System.out.println("sql="+sql);
		try
		{			
			PreparedStatement st1 = getDBConnection().prepareStatement(sql);
			st1.setInt(1,gid);
			ResultSet rs1 = st1.executeQuery();
			if (rs1.next()) {
				int c = rs1.getInt(1);
				System.out.println("c="+c);
				if (c>0) {
					// Row already exists for this group/object
					String updSQL="UPDATE dm."+TableName+" SET "+AccessCol+"=? WHERE "+KeyCol+"=? AND usrgrpid=?";
					System.out.println("updSQL="+updSQL);
					PreparedStatement st2 = getDBConnection().prepareStatement(updSQL);
					st2.setString(1,st);
					st2.setInt(2,id);
					st2.setInt(3,gid);
					st2.execute();
					added = (st2.getUpdateCount()>0);
					st2.close();
				}
				else
				{
					// Row does not exist for this group/object - add it
					String insSQL;
					int lc;
					if (obj.hasReadWrite()) {
						insSQL="INSERT INTO dm."+TableName+"("+KeyCol+",usrgrpid,viewaccess,updateaccess,readaccess,writeaccess) VALUES(?,?,?,?,?,?)";
						lc=6;
					} else {
						insSQL="INSERT INTO dm."+TableName+"("+KeyCol+",usrgrpid,viewaccess,updateaccess) VALUES(?,?,?,?)";
						lc=4;
					}
					System.out.println("insSQL="+insSQL);
					PreparedStatement st3 = getDBConnection().prepareStatement(insSQL);
					st3.setInt(1,id);
					st3.setInt(2,gid);
					for (int i=3;i<=lc;i++) {
						if (i==ycol) {
							st3.setString(i,st);
						} else {
							st3.setNull(i,Types.VARCHAR);
						}
					}
					st3.execute();
					added = (st3.getUpdateCount()>0);
					st3.close();
				}
				// Record Change in object's timeline
				UserGroup grp = getGroup(gid);
				if (grp !=  null && yname != null) {
					// use insertUpdateRecord rather than recordObjectUpdate since this ignores 2 updates in close succession and if
					// a group has been dragged from one access group to another there are two updates
					// one after another (one for remove, one for add)
					insertUpdateRecord(obj,"Group "+grp.getName()+" added to "+yname,0);
				}
			}
			st1.close();
			getDBConnection().commit();
			return added;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	private void RemoveAccessInternal(DMObject obj,String t,int gid,boolean DomainInheritance)
	{
		// domaininherit no longer separate table - just use domainaccess
		// String TableName=DomainInheritance?"dm_domaininherit":obj.getDatabaseTable()+"access";
		String TableName=DomainInheritance?"dm_domainaccess":obj.getDatabaseTable()+"access";
		String KeyCol=obj.getForeignKey();
		String AccessCol="";
		String yname=null;
		boolean inherited=false;
		Hashtable<Integer, ObjectAccess> acl = getAccessForDomain(obj.getDomainId());
		ObjectAccess oa = acl.get(gid);
		
		if (t.equalsIgnoreCase("va")) {
			AccessCol="viewaccess";
			yname="View Access";
			inherited = (oa!=null)?oa.isViewable():false;
		} else if (t.equalsIgnoreCase("ua")) {
			AccessCol="updateaccess";
			yname="Change Access";
			inherited = (oa!=null)?oa.isUpdatable():false;
		} else if (t.equalsIgnoreCase("wa")) {
			AccessCol="writeaccess";
			yname=obj.getWriteTitle();
			inherited = (oa!=null)?oa.isWriteable():false;
		} else if (t.equalsIgnoreCase("ra")) {
			AccessCol="readaccess";
			yname=obj.getReadTitle();
			inherited = (oa!=null)?oa.isReadable():false;
		} else if (t.equalsIgnoreCase("vx")) {
			AccessCol="viewaccess";
			yname="View Access";
			inherited = (oa!=null)?oa.isViewable():false;
		} else if (t.equalsIgnoreCase("ux")) {
			AccessCol="updateaccess";
			yname="Change Access";
			inherited = (oa!=null)?oa.isUpdatable():false;
		} else if (t.equalsIgnoreCase("wx")) {
			AccessCol="writeaccess";
			yname=obj.getWriteTitle();
			inherited = (oa!=null)?oa.isWriteable():false;
		} else if (t.equalsIgnoreCase("rx")) {
			AccessCol="readaccess";
			yname=obj.getReadTitle();
			inherited = (oa!=null)?oa.isReadable():false;
		}
		
		
		int id=obj.getId();
		
		System.out.println("Removing Group from object objtype="+obj.getObjectTypeAsInt()+" objid="+id+" t="+t+" gid="+gid);
		System.out.println("inherited="+inherited);
		String sql="SELECT count(*) FROM dm."+TableName+" WHERE "+KeyCol+"=? AND usrgrpid=? AND "+AccessCol+" is not null";
		System.out.println("sql="+sql);
		try
		{			
			PreparedStatement st1 = getDBConnection().prepareStatement(sql);
			st1.setInt(1,id);
			st1.setInt(2,gid);
			ResultSet rs1 = st1.executeQuery();
			if (rs1.next()) {
				int c = rs1.getInt(1);
				System.out.println("c="+c);
				if (c>0) {
					// Row exists for this group/object/accesstype
					String chkSQL="SELECT "+AccessCol+" FROM dm."+TableName+" WHERE "+KeyCol+"=? AND usrgrpid=?";
					System.out.println("chkSQL="+chkSQL);
					PreparedStatement st2 = getDBConnection().prepareStatement(chkSQL);
					st2.setInt(1,id);
					st2.setInt(2,gid);
					ResultSet rs2=st2.executeQuery();
					if (rs2.next()) {
						String oldAccess = rs2.getString(1);
						System.out.println("oldAccess="+oldAccess);
					}
					rs2.close();
					st2.close();
					String updSQL="UPDATE dm."+TableName+" SET "+AccessCol+"=null WHERE "+KeyCol+"=? AND usrgrpid=?";
					System.out.println("updSQL="+updSQL);
					PreparedStatement st3 = getDBConnection().prepareStatement(updSQL);
					st3.setInt(1,id);
					st3.setInt(2,gid);
					st3.execute();
					st3.close();
					//
					// Remove the row if there are no more significant values left
					//
					String delSQL;
					if (obj.hasReadWrite()) {
						delSQL="DELETE FROM dm."+TableName+" WHERE "+KeyCol+"=? AND usrgrpid=? AND viewaccess is null AND updateaccess is null AND writeaccess is null AND readaccess is null";
					} else {
						delSQL="DELETE FROM dm."+TableName+" WHERE "+KeyCol+"=? AND usrgrpid=? AND viewaccess is null AND updateaccess is null";
					}
					 
					PreparedStatement st4 = getDBConnection().prepareStatement(delSQL);
					st4.setInt(1,id);
					st4.setInt(2,gid);
					st4.execute();
					st4.close();
					// Record to the object's timeline
					UserGroup grp = getGroup(gid);
					if (grp != null && yname != null) {
						if (inherited) {
							// use insertUpdateRecord rather than recordObjectUpdate since this ignores 2 updates in close succession and if
							// a group has been dragged from one access group to another there are two updates
							// one after another (one for remove, one for add)
							insertUpdateRecord(obj,"Inherited "+yname+" restored to Group "+grp.getName(),0);
						} else {
							insertUpdateRecord(obj,"Group "+grp.getName()+" removed from "+yname,0);
						}
					}
				}
				else
				{
					// Row not found - since it was clearly there when the screen was displayed then either (a) the row has been deleted
					// by someone else or (b) this is an inherited permission. If it's an inherited permission then we need to insert a
					// row here with a "N" flag in the appropriate place since the user presumably wants to override the inherited permission
					// and remove access
					//
					
					
					String cSQL="SELECT count(*) FROM dm."+TableName+" WHERE "+KeyCol+"=? AND usrgrpid=?";
					System.out.println(cSQL);
					System.out.println("id="+id+" gid="+gid);
					PreparedStatement st4 = getDBConnection().prepareStatement(cSQL);
					st4.setInt(1,id);
					st4.setInt(2,gid);
					ResultSet rs4 = st4.executeQuery();
					if (rs4.next()) {
						int c2=rs4.getInt(1);
						System.out.println("c2="+c2);
						if (c2>0) {
							// Row already exists for this object and group - set the access type to N
							String uSQL="UPDATE dm."+TableName+" set "+AccessCol+"='N' WHERE "+KeyCol+"=? AND usrgrpid=?";
							PreparedStatement st5 = getDBConnection().prepareStatement(uSQL);
							st5.setInt(1,id);
							st5.setInt(2,gid);
							st5.execute();
							st5.close();
							// Record to the object's timeline
							UserGroup grp = getGroup(gid);
							if (grp != null && yname != null) {
								if (inherited) {
									insertUpdateRecord(obj,"Inherited "+yname+" denied to group "+grp.getName(),0);
								} else {
									insertUpdateRecord(obj,"Group "+grp.getName()+" removed from "+yname,0);
								}
							}
						}
						else
						{
							String iSQL="INSERT INTO dm."+TableName+"("+KeyCol+",usrgrpid,"+AccessCol+") VALUES(?,?,'N')";
							PreparedStatement st6 = getDBConnection().prepareStatement(iSQL);
							st6.setInt(1,id);
							st6.setInt(2,gid);
							st6.execute();
							st6.close();
							// Record to the object's timeline
							UserGroup grp = getGroup(gid);
							if (grp != null && yname != null) {
								if (inherited) {
									insertUpdateRecord(obj,"Inherited "+yname+" denied to group "+grp.getName(),0);
								} else {
									insertUpdateRecord(obj,"Group "+grp.getName()+" removed from "+yname,0);
								}
							}
						}
					}
					rs4.close();
					st4.close();
				}
			}
			st1.close();
			getDBConnection().commit();
			return;
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		
		}
	}
	
	public void RemoveAccess(DMObject obj,String t,int gid,boolean DomainInheritance)
	{
		RemoveAccessInternal(obj,t,gid,DomainInheritance);
	}
	
	public void RemoveDenyAccess(DMObject obj,String t,int gid,boolean DomainInheritance)
	{
		RemoveAccessInternal(obj,t,gid,DomainInheritance);
	}
	
	public void setUserPermissions(int groupid,UserPermissions up)
	{
		if (groupid>0) {
			try
			{
				// Setting permissions for a specific group
				PreparedStatement stmt = getDBConnection().prepareStatement("SELECT cobjtype FROM dm.dm_privileges WHERE groupid=?");
				stmt.setInt(1, groupid);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					ObjectType x = ObjectType.fromInt(rs.getInt(1));
					if(x != null) {
						switch (x) {
						case USER:			up.setCreateUsers(true); break;
						case USERGROUP:		up.setCreateGroups(true); break;
						case DOMAIN:		up.setCreateDomains(true); break;
						case ENVIRONMENT:	up.setCreateEnvs(true); break;
						case SERVER:		up.setCreateServers(true); break;
						case REPOSITORY:	up.setCreateRepos(true); break;
						case APPLICATION:	up.setCreateApps(true); break;
						case APPVERSION:	up.setCreateAppvers(true); break;
						case COMPONENT:		up.setCreateComps(true); break;
						case CREDENTIALS:	up.setCreateCreds(true); break;
						case ACTION:		up.setCreateActions(true); break;
						case PROCEDURE:		up.setCreateProcs(true); break;
						case DATASOURCE:	up.setCreateDatasrc(true); break;
						case NOTIFY:		up.setCreateNotifiers(true); break;
						case BUILDER:		up.setCreateEngines(true); break;
						default: break;
						}
					}
				}
				rs.close();
				stmt.close();
			}
			catch(SQLException e)
			{
				e.printStackTrace();
				rollback();
			}
		}
	}
	
	public boolean getCreatePermission(ObjectType objtype)
	{
		try
		{
			boolean res=false;
			PreparedStatement stmt = getDBConnection().prepareStatement("SELECT count(*) FROM dm.dm_privileges a,dm.dm_usersingroup b WHERE a.groupid=b.groupid AND a.cobjtype=? AND b.userid=?");
			System.out.println("Looking for object type "+objtype.value()+" with userid="+getUserID());
			stmt.setInt(1, objtype.value());
			stmt.setInt(2, getUserID());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int count = rs.getInt(1);
				System.out.println("count="+count);
				if (count > 0) res = true;
			}
			rs.close();
			stmt.close();
			System.out.println("returning res="+res);
			return res;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public void setPermissionsForGroup(int groupid,UserPermissions up)
	{
		try
		{
			System.out.println("setPermissionsForGroup - enter");
			List <ObjectType> ots = new ArrayList<ObjectType>();
			PreparedStatement stmt = getDBConnection().prepareStatement("DELETE FROM dm.dm_privileges WHERE groupid=?");
			stmt.setInt(1,groupid);
			stmt.execute();
			stmt.close();
			
			stmt = getDBConnection().prepareStatement("INSERT INTO dm.dm_privileges(groupid,cobjtype) VALUES(?,?)");
			stmt.setInt(1,groupid);
			
			if (up.getCreateUsers())     ots.add(ObjectType.USER);
			if (up.getCreateGroups())    ots.add(ObjectType.USERGROUP);
			if (up.getCreateDomains())   ots.add(ObjectType.DOMAIN); 
			if (up.getCreateEnvs())      ots.add(ObjectType.ENVIRONMENT);
			if (up.getCreateServers())   ots.add(ObjectType.SERVER);
			if (up.getCreateRepos())     ots.add(ObjectType.REPOSITORY);
			if (up.getCreateComps())     ots.add(ObjectType.COMPONENT);
			if (up.getCreateCreds())     ots.add(ObjectType.CREDENTIALS);
			if (up.getCreateApps())      ots.add(ObjectType.APPLICATION);
			if (up.getCreateAppvers())   ots.add(ObjectType.APPVERSION);
			if (up.getCreateActions())   ots.add(ObjectType.ACTION);
			if (up.getCreateProcs())     ots.add(ObjectType.PROCEDURE);
			if (up.getCreateDatasrc())   ots.add(ObjectType.DATASOURCE);
			if (up.getCreateNotifiers()) ots.add(ObjectType.NOTIFY);
			if (up.getCreateEngines())   ots.add(ObjectType.BUILDER);

			for (ObjectType ot: ots) {
				System.out.println("adding "+ot.name()+" value "+ot.value());
				stmt.setInt(2,ot.value());
				stmt.execute();
			}
			stmt.close();
			getDBConnection().commit();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		System.out.println("Set permissions for group - exit");
	}
	
	public void setGroupTabAccess(int groupid,String AclOverride,String endpoints,String applications,String actions,String providers,String users)
	{
		DynamicQueryBuilder query = new DynamicQueryBuilder(getDBConnection(),"UPDATE dm.dm_usergroup SET ");
		query.add("modified = ?, modifierid = ?", timeNow(), getUserID());
		
		if (AclOverride != null) {
			query.add(", acloverride=?", AclOverride);
		}
		if (endpoints != null) {
			query.add(", tabendpoints=?", endpoints);
		}
		if (applications != null) {
			query.add(", tabapplications=?", applications);
		}
		if (actions != null) {
			query.add(", tabactions=?", actions);
		}
		if (providers != null) {
			query.add(", tabproviders=?", providers);
		}
		if (users != null) {
			query.add(", tabusers=?", users);
		}
		
		query.add(" WHERE id = ?", groupid);
		
		try {
			query.execute();
			getDBConnection().commit();
			query.close();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
	}
	
	// Notifier Templates
	public List<UserGroup> getGroupsForTemplate(int templateid,boolean inTemplate)
	{
		List<UserGroup> ret = new ArrayList<UserGroup>();
		String sql = null;;
		
		if (!inTemplate) {
			sql = "SELECT a.id,a.name FROM dm.dm_usergroup a WHERE a.status<>'D' AND a.domainid in ("+m_domainlist+") and not exists (select b.usrgrpid from dm.dm_templaterecipients b where b.templateid=? AND a.id=b.usrgrpid AND b.usrgrpid is not null) order by a.name";
		} else {
			sql = "SELECT a.id,a.name FROM dm.dm_usergroup a,dm.dm_templaterecipients b where a.status<>'D' AND a.id=b.usrgrpid and b.templateid=? order by a.name";
		}
		try {
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,templateid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				UserGroup ug = new UserGroup();
				ug.setId(rs.getInt(1));
				ug.setName(rs.getString(2));
				ret.add(ug);
			}
			rs.close();
			stmt.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		return ret;
	}
	public List<User> getUsersForTemplate(int templateid,boolean inTemplate)
	{
		String[] sdesc = new String[3];
		int[] special = new int[3];
		
		sdesc[0] = "${server.owner}";
		sdesc[1] = "${application.owner}";
		sdesc[2] = "${environment.owner}";
		
		List<User> ret = new ArrayList<User>();
		String sql;
		String sqlx = "SELECT ownertype FROM dm.dm_templaterecipients WHERE templateid=? AND ownertype IS NOT NULL";
		
		if (!inTemplate) {
			sql = "SELECT a.id,a.name FROM dm.dm_user a WHERE a.domainid in ("+m_domainlist+") and not exists (select b.userid from dm.dm_templaterecipients b where b.templateid=? and a.id=b.userid and b.userid is not null) ORDER BY name"
;		} else {
			sql = "SELECT a.id,a.name FROM dm.dm_user a,dm.dm_templaterecipients b where a.id=b.userid and b.templateid=? order by a.name";
		}
		
		try {
			PreparedStatement stmtx = getDBConnection().prepareStatement(sqlx);
			stmtx.setInt(1,templateid);
			ResultSet rsx = stmtx.executeQuery();
			while (rsx.next())
			{
				int sid = rsx.getInt(1);
				special[sid-1]=1;
				if (inTemplate) {
					User t = new User();
					t.setId(0-sid);
					t.setName(sdesc[sid-1]);
					ret.add(t);
				}
			}
			rsx.close();
			stmtx.close();
			if (!inTemplate) {
				// add the specials that are not being used by the template
				for (int i=0;i<sdesc.length;i++) {
					if (special[i]==0) {
						User t = new User();
						t.setId(0-(i+1));
						t.setName(sdesc[i]);
						ret.add(t);
					}
				}
			}
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,templateid);
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				User u = new User();
				u.setId(rs.getInt(1));
				u.setName(rs.getString(2));
				ret.add(u);
			}
			rs.close();
			stmt.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	private void addRecipientToTemplate(String colname,int templateid,int id)
	{
		try
		{
			String csql="SELECT count(*) FROM dm.dm_templaterecipients WHERE "+colname+"=? AND templateid=?";
			String isql="INSERT INTO dm.dm_templaterecipients(templateid,"+colname+") VALUES (?,?)";
			String usql="UPDATE dm.dm_template set modified=?, modifierid=? WHERE id=?";
			PreparedStatement cstmt = getDBConnection().prepareStatement(csql);
			cstmt.setInt(1, id);
			cstmt.setInt(2,templateid);
			ResultSet crs = cstmt.executeQuery();
			if (crs.next()) {
				// got the count (should never fail) - if > 0 already there, otherwise we insert
				if (crs.getInt(1)==0) {
					// not already there
					PreparedStatement istmt = getDBConnection().prepareStatement(isql);
					istmt.setInt(1,templateid);
					istmt.setInt(2,id);
					istmt.execute();
					if (istmt.getUpdateCount()>0) {
						// Updated - change the last modified time of the template
						PreparedStatement ustmt = getDBConnection().prepareStatement(usql);
						ustmt.setLong(1,timeNow());
						ustmt.setInt(2,getUserID());
						ustmt.setInt(3,templateid);
						ustmt.execute();
						ustmt.close();
					}
					getDBConnection().commit();
					istmt.close();
				}
			}
			crs.close();
			cstmt.close();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	private void removeRecipientFromTemplate(String colname,int templateid,int id)
	{
		try
		{
			String dsql="DELETE FROM dm.dm_templaterecipients WHERE templateid=? AND "+colname+"=?";
			
			PreparedStatement dstmt = getDBConnection().prepareStatement(dsql);
			dstmt.setInt(1,templateid);
			dstmt.setInt(2, id);
			dstmt.execute();
			dstmt.close();

			String usql="UPDATE dm.dm_template set modified=?, modifierid=? WHERE id=?";
			
			PreparedStatement ustmt = getDBConnection().prepareStatement(usql);
			ustmt.setLong(1,timeNow());
			ustmt.setInt(2,getUserID());
			ustmt.setInt(3,templateid);
			ustmt.execute();
			ustmt.close();
			
			getDBConnection().commit();

		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public void addUserToTemplate(int templateid,int userid)
	{
		addRecipientToTemplate("userid",templateid,userid);
	}
	
	public void addGroupToTemplate(int templateid,int groupid)
	{
		addRecipientToTemplate("usrgrpid",templateid,groupid);
	}
	
	public void addSpecialToTemplate(int templateid,int specialid)
	{
		addRecipientToTemplate("ownertype",templateid,specialid);
	}
	
	public void removeUserFromTemplate(int templateid,int userid)
	{
		removeRecipientFromTemplate("userid",templateid,userid);
	}
	
	public void removeGroupFromTemplate(int templateid,int groupid)
	{
		removeRecipientFromTemplate("usrgrpid",templateid,groupid);
	}
	
	public void removeSpecialFromTemplate(int templateid,int specialid)
	{
		removeRecipientFromTemplate("ownertype",templateid,specialid);
	}
	
	public void saveTemplateBody(int templateid,String subject,String body)
	{
		try
		{
			long t = timeNow();
			String usql="UPDATE dm.dm_template set modified=?, modifierid=?, subject=?,body=? WHERE id=?";
			System.out.println("usql="+usql+" templateid="+templateid);
			PreparedStatement ustmt = getDBConnection().prepareStatement(usql);
			ustmt.setLong(1,t);
			ustmt.setInt(2,getUserID());
			ustmt.setString(3,subject);
			ustmt.setString(4,body);
			ustmt.setInt(5,templateid);
			ustmt.execute();
			System.out.println("update count="+ustmt.getUpdateCount());
			getDBConnection().commit();
			ustmt.close();
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	public List<NotifyTemplate> getAccessibleTemplates(String objid,int domainid)
	{
		System.out.println("getAccessibleTemplates("+objid+","+domainid+")");
		if (objid.startsWith("task")) {
			objid="ta"+objid.substring(4);
		}
		ObjectTypeAndId x = new ObjectTypeAndId(objid);
		Domain d = null;
		if (x.getId()>0) {
			// Get domain from object
			DMObject obj = this.getObject(x.getObjectType(), x.getId());
			System.out.println("obj = "+obj.getName());
			d =  obj.getDomain();
		} else {
			// New object - get domain from tree
			d = getDomain(domainid);
		}
		Hashtable<Integer,char[]> accessRights = new Hashtable<Integer,char[]>();
		try
		{
			System.out.println("getting templates for domain "+d.getId());
			String sql1 = "select a.id,b.viewaccess,b.readaccess	"
					+	"from	dm.dm_notify		a,	"
					+	"		dm.dm_notifyaccess	b,	"
					+	"		dm.dm_usersingroup	c	"
					+	"where	c.userid=?				"
					+	"and	c.groupid=b.usrgrpid	"
					+	"and	a.id=b.notifyid			"
					+	"and	a.domainid=?			"
					+	"union	"
					+	"select	d.id,e.viewaccess,e.readaccess	"
					+	"from	dm.dm_notify		d,	"
					+	"		dm.dm_notifyaccess	e	"
					+	"where	e.usrgrpid=1			"	// user group 1 = Everyone
					+	"and	d.id=e.notifyid			"
					+	"and	d.domainid=?			";
			
			String sql2 = "SELECT n.id, t.id, t.name, n.domainid "
					+ "FROM dm.dm_template t,dm.dm_notify n WHERE n.domainid =? AND n.status = 'N' "
					+ "AND t.status = 'N' AND t.notifierid = n.id ORDER BY 2";
			
			List<NotifyTemplate> ret = new ArrayList<NotifyTemplate>();
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			while (d != null && d.getId()>=1) {
				System.out.println("domain is "+d.getName()+") read="+d.isReadable(true)+" write="+d.isWriteable(true)+" update="+d.isUpdatable(true)+" view="+d.isViewable(true));
				if (!m_OverrideAccessControl) {
					// Get a list of actions in this domain with non-default access permissions
					System.out.println("no override access, getting list of overrides userid="+getUserID()+" domainid="+d.getId());
					stmt1.setInt(1,getUserID());
					stmt1.setInt(2,d.getId());
					stmt1.setInt(3,d.getId());
					ResultSet rs = stmt1.executeQuery();
					while (rs.next()) {
						char[] ar = new char[2];
						ar[0] = getString(rs,2,"-").charAt(0);
						ar[1] = getString(rs,3,"-").charAt(0);
						System.out.println("notifyaccess) id="+rs.getInt(1)+" ar[0]="+ar[0]+" ar[1]="+ar[1]);
						accessRights.put(rs.getInt(1),ar);
					}
					rs.close();
				}
				stmt2.setInt(1,d.getId());
				ResultSet rs = stmt2.executeQuery();
				while(rs.next()) {
					boolean include=false;
					int notifierid = rs.getInt(1);
					NotifyTemplate t = new NotifyTemplate(this, rs.getInt(2), rs.getString(3));
					if (!m_OverrideAccessControl) {
						// Need to check permissions on this notifier. Only include it if it's both
						// viewable and readable (send rights)
						char[] ar = accessRights.get(notifierid);
						if (ar != null) {
							System.out.println(t.getId()+") ar[0]="+ar[0]+" ar[1]="+ar[1]);
							boolean viewable=(ar[0]=='Y')?true:(ar[0]=='-')?d.isViewable(true):false; 
							boolean sendable=(ar[1]=='Y')?true:(ar[1]=='-')?d.isReadable(true):false; 
							System.out.println("viewable="+viewable+" sendable="+sendable);
							if (viewable && sendable) include=true;
						} else {
							// No override record
							System.out.println("no override record for "+t.getId()+" ("+t.getName()+"), taking domain permissions d.isViewable(true)="+d.isViewable(true)+" d.isReadable(true)="+d.isReadable(true));
							include = (d.isViewable(true) && d.isReadable(true));
						}
					} else {
						include=true;	// override access control
					}
					if (include) {
						t.setDomainId(rs.getInt(4));
						ret.add(t);
					} else {
						// debug
						System.out.println("Not adding template "+t.getName());
					}
				}
				rs.close();
				d=d.getDomain();
			}
			stmt2.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve templates from database");	
		
		
		
		
		
		
		
		
		
		/*
		
		String sql = "SELECT t.id, t.name "
				+ "FROM dm.dm_template t,dm.dm_notify n WHERE n.domainid in (" + m_domainlist + ") AND n.status = 'N' "
				+ "AND t.status = 'N' AND t.notifierid = n.id ORDER BY 2";

		try
		{
			PreparedStatement stmt = m_conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();
			List<NotifyTemplate> ret = new ArrayList<NotifyTemplate>();
			while(rs.next()) {
				NotifyTemplate t = new NotifyTemplate(this, rs.getInt(1), rs.getString(2));
				ret.add(t);
			}
			rs.close();
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to retrieve templates from database");	
		*/		
	}
	
	//
	// Task Validatation
	//
	
	public class TaskValidateException extends Exception {
		private static final long serialVersionUID = 1L;
		String m_issue;
		public TaskValidateException(String message){
		     m_issue = message;
		}
		public String getIssue() { return m_issue; }
	}
	
	private String ValidateDeployPermission(int appid,int envid)
	{
		Environment env = getEnvironment(envid,false);
		Application app = getApplication(appid,false);
		if (!app.isWriteable()) return "You do not have permission to deploy this application";
		if (!env.isReadable()) return "You do not have permission to deploy to this environment";
		return null;
	}
	
	String ValidateDeploy(int appid,int envid)
	{
		String sql1  = "SELECT calusage FROM dm.dm_environment WHERE id=?";
		String sql2  = "SELECT count(*) FROM dm.dm_availability WHERE envid=? AND unavailstart <= ? AND unavailend >= ?";
		String sql3  = "SELECT count(*) FROM dm.dm_calendar WHERE status <> 'D' AND envid=? AND appid=? AND starttime <= ? AND endtime >=?";
		String sql3a = "SELECT count(*) FROM dm.dm_calendar WHERE status <> 'D' AND envid=? AND starttime <= ? AND endtime >=? AND appid = (SELECT parentid FROM dm.dm_application WHERE id=?);";
		String sql4  = "SELECT count(*) FROM dm.dm_calendar WHERE status <> 'D' AND envid=? AND starttime <= ? AND endtime >=?";
		
		Environment env = getEnvironment(envid,false);
		// Application app = getApplication(appid,false);
		
		try
		{
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,envid);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				String calusage = rs1.getString(1);
				if( rs1.wasNull()) calusage="e";
				rs1.close();
				stmt1.close();
				if (calusage.equalsIgnoreCase("x")) {
					// Environment has been marked as "unavailable"
					throw new TaskValidateException("Cannot deploy to environment \""+env.getName()+"\": environment has been marked as \"unavailable\".");
				}
				else
				{
					// Check if there's an "unavailable" slot occupying current time
					
					Calendar sow = new GregorianCalendar();
					int dow = sow.get(Calendar.DAY_OF_WEEK);	// Day of Week 1 (Sun) to 7 (Sat)
					int dom = 0;
					switch(dow) {
					case Calendar.SUNDAY:
						dom = 11;	// Sun 11th Jan 1970
						break;
					case Calendar.MONDAY:
						dom = 5;	// Mon 5th Jan 1970
						break;
					case Calendar.TUESDAY:
						dom = 6;	// Tue 6th Jan 1970
						break;
					case Calendar.WEDNESDAY:
						dom = 7;	// Wed 7th Jan 1970
						break;
					case Calendar.THURSDAY:
						dom = 8;	// Thu 8th Jan 1970
						break;
					case Calendar.FRIDAY:
						dom = 9;	// Fri 9th Jan 1970
						break;
					case Calendar.SATURDAY:
						dom = 10;	// Sat 10th Jan 1970
						break;
					}
					sow.set(Calendar.MONTH, Calendar.JANUARY);
					sow.set(Calendar.YEAR, 1970);
					sow.set(Calendar.DAY_OF_MONTH,dom);
					sow.set(Calendar.ZONE_OFFSET,0);
					// 345600 is num of secs from 1/1/1970 to 5/1/1970 (start of calendar)
					long fromsow = (sow.getTimeInMillis() / 1000) - 345600; // this calcs offset in seconds from midnight on Monday
					PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
					stmt2.setInt(1,envid);
					stmt2.setLong(2,fromsow);
					stmt2.setLong(3,fromsow);
					ResultSet rs2 = stmt2.executeQuery();
					int c = 0;
					if (rs2.next()) c = rs2.getInt(1);
					rs2.close();
					stmt2.close();
					if (c>0) throw new TaskValidateException("Environment \""+env.getName()+"\" is currently unavailable");
					//
					// Check to see if there's currently a calendar event for this application
					//
					long now = timeNow();
					PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
					stmt3.setInt(1,envid);
					stmt3.setInt(2,appid);
					stmt3.setLong(3,now);
					stmt3.setLong(4,now);
					ResultSet rs3 = stmt3.executeQuery();
					c = 0;
					if (rs3.next()) c = rs3.getInt(1);
					rs3.close();
					stmt3.close();
					if (c>0) return ValidateDeployPermission(appid,envid);	// No error (there's an entry in the calendar for this app)
					//
					// Check if the application in the calendar is an app and not a version. If so, allow any
					// application version of that app to be deployed
					//
					System.out.println("Didn't find exact match - checking envid="+envid+" appid="+appid+" now="+now);
					PreparedStatement stmt3a = getDBConnection().prepareStatement(sql3a);
					stmt3a.setInt(1,envid);
					stmt3a.setLong(2,now);
					stmt3a.setLong(3,now);
					stmt3a.setInt(4,appid);
					ResultSet rs3a = stmt3a.executeQuery();
					c = 0;
					if (rs3a.next()) c = rs3a.getInt(1);
					rs3a.close();
					stmt3a.close();
					System.out.println("c is "+c);
					if (c>0) return ValidateDeployPermission(appid,envid);	// No error (there's an entry in the calendar for this app)
					if (calusage.equalsIgnoreCase("o")) {
						// Environment is always available except when calendar entries say otherwise
						PreparedStatement stmt4 = getDBConnection().prepareStatement(sql4);
						stmt4.setInt(1,envid);
						stmt4.setLong(2,now);
						stmt4.setLong(3,now);
						ResultSet rs4 = stmt4.executeQuery();
						c = 0;
						if (rs4.next()) c = rs4.getInt(1);
						rs4.close();
						stmt4.close();
						if (c>0) throw new TaskValidateException("Cannot deploy to environment \""+env.getName()+"\": The environment is reserved for a different application package or version");
					}
					else
					if (calusage.equalsIgnoreCase("e")) {
						// Environment is only available for deployment when there's a calendar entry giving us permission
						throw new TaskValidateException("Cannot deploy to environment \""+env.getName()+"\": There is no entry in the calendar for this application package or version");
					}
					// success
					return ValidateDeployPermission(appid,envid);
				}
			} else {
				rs1.close();
				stmt1.close();
				throw new TaskValidateException("Failed to find calusage for envid "+envid);
			}
		}
		catch(TaskValidateException ex)
		{
			return ex.getIssue();
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to ValidateDeploy");			
	}
	
	String ValidateMove(int appid,int tgtid,int srcid)
	{	
		Application app = getApplication(appid,false);
		Domain tgt = getDomain(tgtid);
		
		System.out.println("Checking appid="+appid+" source domain="+srcid+" target domain="+tgtid);

		String sql1 = "SELECT count(*) FROM dm.dm_task a,dm.dm_tasktypes b WHERE a.domainid=? AND a.typeid=b.id and b.name='Approve'";
		String sql2 = "SELECT count(*) FROM dm.dm_approval WHERE appid=? AND approved='Y' AND domainid=? AND "+whencol+" = "
				+		"(SELECT max("+whencol+") FROM dm.dm_approval WHERE appid=? AND domainid=?)";
		
		try
		{
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,srcid);
			ResultSet rs1 = stmt1.executeQuery();
			int c = 0;
			if (rs1.next()) c = rs1.getInt(1);
			rs1.close();
			stmt1.close();
			if (c == 0) return null;	// There is no approve task in this domain - allow to run
			//
			// Check for approval
			//
			PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			stmt2.setInt(1,appid);
			stmt2.setInt(2,tgtid);
			stmt2.setInt(3,appid);
			stmt2.setInt(4,tgtid);
			ResultSet rs2 = stmt2.executeQuery();
			if (rs2.next()) {
				c = rs2.getInt(1);
				if (c==0) {
					rs2.close();
					stmt2.close();
					throw new TaskValidateException("Cannot move "+app.getName()+": it is not approved for domain "+tgt.getName());
				}
			}
			rs2.close();
			stmt2.close();
			return null;
		}
		catch(TaskValidateException ex)
		{
			return ex.getIssue();
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		throw new RuntimeException("Unable to ValidateMove");
	}
	
	boolean isTaskInDomain(int domainid,TaskType tt)
	{
		System.out.println("domainid="+domainid+" tt="+tt.toString().replace("_","")+" userid="+getUserID());
		String sql = 	"SELECT count(*) FROM dm.dm_task a, "
					+	"dm.dm_tasktypes b,	"
					+	"dm.dm_taskaccess c,	"
					+	"dm_usersingroup d	"
					+	"WHERE a.domainid=?	"
					+	"AND a.typeid=b.id and lower(b.name)=lower(?)	"
					+	"AND c.taskid=a.id AND c.usrgrpid in (d.groupid,1)	"
					+	"AND d.userid=?";
		try
		{
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			stmt.setInt(1,domainid);
			stmt.setString(2,tt.toString().replace("_",""));
			stmt.setInt(3,getUserID());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				System.out.println("createversion count="+rs.getInt(1));
				return rs.getInt(1)>0;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	boolean CheckSubscription(String ot,int id)
	{
		ObjectType kind = ObjectType.DOMAIN;
		if (ot.equalsIgnoreCase("environment")) kind=ObjectType.ENVIRONMENT;
		else
		if (ot.equalsIgnoreCase("application")) kind=ObjectType.APPLICATION;
		else
		if (ot.equalsIgnoreCase("appversion")) kind=ObjectType.APPVERSION;
		else
	 if (ot.equalsIgnoreCase("release")) kind=ObjectType.RELEASE;
	 else
	 if (ot.equalsIgnoreCase("relversion")) kind=ObjectType.RELVERSION;
		
		try
		{
			String sql = "SELECT count(*) FROM dm.dm_historysubs WHERE id=? AND kind=? AND userid=?";
			PreparedStatement stmt = getDBConnection().prepareStatement(sql);
			System.out.println("Checking id="+id+" kind="+kind.value()+" userid="+getUserID());
			stmt.setInt(1,id);
			stmt.setInt(2,kind.value());
			stmt.setInt(3,getUserID());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				System.out.println("count="+rs.getInt(1));
				return rs.getInt(1)>0;
			}
			return false;
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
			rollback();
		}
		return false;
	}
	
	String ChangePassword(String oldpass,String newpass)
	{
		//
		// Changing the password for the logged in user. If the user is changing the password because
		// "forcechange" is set, then we don't need the old password (since we'll have just logged in)
		//
		String res=null;
		String hash=null;
		try
		{
			System.out.println("Verifying old password ("+oldpass+")");
			PreparedStatement st = getDBConnection().prepareStatement("SELECT passhash FROM dm.dm_user where id = ?");
			st.setInt(1,getUserID());
			ResultSet rs = st.executeQuery();
			if (!rs.next()) {
				res="Failed to get password from DB";
			} else {
				hash = rs.getString(1);
				if (oldpass != null) {
					String base64pw = encryptPassword(oldpass);
					// Compare the encrypted passwords
					if((hash == null) || (!base64pw.equals(hash))) {
						res="Old password incorrect - please try again";
					}
				}
			}
			rs.close();
			st.close();		

			if (res == null) {
				// old password has been verified or was not needed
				if (newpass.length()<4) {
					res="Password must be at least 4 characters long";
				} else {
					// Try encoding it
					String newbase64pw = encryptPassword(newpass);
					if (hash != null && newbase64pw.equals(hash)) {
						res="New password cannot be the same as existing password";
					} else {
						// New password is encrypted and is different to original - update password.
						PreparedStatement st2 = getDBConnection().prepareStatement("UPDATE dm.dm_user SET passhash=?,forcechange='N' WHERE id = ?");
						st2.setString(1,newbase64pw);
						st2.setInt(2,getUserID());
						st2.execute();
						if (st2.getUpdateCount()<1) {
							res="Failed to update password in database";
						} else {
							getDBConnection().commit();
						}
						st2.close();
					}
				}
			}
		}
		catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return res;
	}
	
	// Copy/Paste
	public void CopyObject(String objtype,int id)
	{
		setCopyObjType(objtype);
		setCopyId(id);
	}
	
	public String getPasteObjectType()
	{
		return getCopyObjType();
	}
	
	public int getPasteObjectId()
	{
		return getCopyId();
	}

	
	public boolean CopyAttributes(String TableName,String foreignkey,int oldid,int newid)
	{
		System.out.println("CopyAttributes(\""+TableName+"\",\""+foreignkey+"\","+oldid+","+newid);
		try
		{
			String arrsql = "SELECT arrayid FROM "+TableName+" WHERE arrayid IS NOT NULL AND "+foreignkey+"=?";
			PreparedStatement st1 = getDBConnection().prepareStatement(arrsql);
			st1.setInt(1,oldid);
			ResultSet rs1 = st1.executeQuery();
			while (rs1.next()) {
				// Take a copy of the array
				int newarrid = getID("arrayvalues");
				String coparr = "INSERT INTO dm.dm_arrayvalues(id,name,value) SELECT ?,name,value FROM dm.dm_arrayvalues WHERE id=?";
				PreparedStatement sta = getDBConnection().prepareStatement(coparr);
				sta.setInt(1,newarrid);
				sta.setInt(2,rs1.getInt(1));
				sta.execute();
			}
			rs1.close();
			String varsql = "INSERT INTO "+TableName+"("+foreignkey+",name,value,arrayid,nocase) SELECT ?,name,value,arrayid,nocase FROM "+TableName+" WHERE "+foreignkey+"=?";
			PreparedStatement st2 = getDBConnection().prepareStatement(varsql);
			st2.setInt(1,newid);
			st2.setInt(2,oldid);
			st2.execute();
			st2.close();
			return true;
		}
		catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean CopyServerAttributes(int oldid,int newid)
	{
		System.out.println("CopyServerAttributes");
		try
		{
			String selsql = "SELECT hostname,protocol,notes,basedir,typeid,uname FROM dm.dm_server WHERE id=?";
			String updsql = "UPDATE dm.dm_server SET hostname=?, protocol=?, notes=?, basedir=?,typeid=?,uname=? WHERE id=?";
			String cpysql1 = "INSERT INTO dm.dm_servercomptype(serverid,comptypeid) SELECT ?,comptypeid FROM dm.dm_servercomptype WHERE serverid=?";
			PreparedStatement st1 = getDBConnection().prepareStatement(selsql);
			PreparedStatement st2 = getDBConnection().prepareStatement(updsql);
			PreparedStatement st3 = getDBConnection().prepareStatement(cpysql1);
			st1.setInt(1,oldid);
			ResultSet rs1 = st1.executeQuery();
			while (rs1.next()) {
				st2.setString(1,rs1.getString(1));
				st2.setString(2,rs1.getString(2));
				st2.setString(3,rs1.getString(3));
				st2.setString(4,rs1.getString(4));
				st2.setInt(5,rs1.getInt(5));
				st2.setString(6,rs1.getString(6));
				st2.setInt(7,newid);
				st2.execute();
			}
			rs1.close();
			st1.close();
			st2.close();
			st3.setInt(1,newid);
			st3.setInt(2,oldid);
			st3.execute();
			return CopyAttributes("dm.dm_servervars","serverid",oldid,newid);
		}
		catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public boolean CopyBuilderAttributes(int oldid,int newid)
	{
		System.out.println("CopyBuilderAttributes");
		/*
		try
		{
			String selsql = "SELECT hostname,protocol,notes,basedir,typeid,uname FROM dm.dm_server WHERE id=?";
			String updsql = "UPDATE dm.dm_server SET hostname=?, protocol=?, notes=?, basedir=?,typeid=?,uname=? WHERE id=?";
			PreparedStatement st1 = m_conn.prepareStatement(selsql);
			PreparedStatement st2 = m_conn.prepareStatement(updsql);
			PreparedStatement st3 = m_conn.prepareStatement(cpysql1);
			PreparedStatement st4 = m_conn.prepareStatement(cpysql2);
			st1.setInt(1,oldid);
			ResultSet rs1 = st1.executeQuery();
			while (rs1.next()) {
				st2.setString(1,rs1.getString(1));
				st2.setString(2,rs1.getString(2));
				st2.setString(3,rs1.getString(3));
				st2.setString(4,rs1.getString(4));
				st2.setInt(5,rs1.getInt(5));
				st2.setString(6,rs1.getString(6));
				st2.setInt(7,newid);
				st2.execute();
			}
			rs1.close();
			st1.close();
			st2.close();
			st3.setInt(1,newid);
			st3.setInt(2,oldid);
			st3.execute();
			st4.setInt(1,newid);
			st4.setInt(2,oldid);
			st4.execute();
			return CopyAttributes("dm.dm_servervars","serverid",oldid,newid);
		}
		catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
		*/
		return true;
	}
	
	private boolean CopyComponentAttributes(int oldid,int newid)
	{
		try {
			// Copy Component Items
			HashMap<Integer, Integer> predmap = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> ci1map = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> ci2map = new HashMap<Integer, Integer>();
			String dasql[] = {
					"UPDATE dm.dm_component SET deployalways=(SELECT deployalways FROM dm.dm_component WHERE id=?) WHERE id=?",
					"UPDATE dm.dm_component SET preactionid=(SELECT preactionid FROM dm.dm_component WHERE id=?) WHERE id=?",
					"UPDATE dm.dm_component SET postactionid=(SELECT postactionid FROM dm.dm_component WHERE id=?) WHERE id=?",
					"UPDATE dm.dm_component SET actionid=(SELECT actionid FROM dm.dm_component WHERE id=?) WHERE id=?",
					"UPDATE dm.dm_component SET comptypeid=(SELECT comptypeid FROM dm.dm_component WHERE id=?) WHERE id=?"
			};
			String compsql = "SELECT id,repositoryid,target,name,summary,predecessorid,xpos,ypos,creatorid,created,modifierid,modified,status,rollup,rollback FROM dm.dm_componentitem WHERE compid=?";
			String inssql = "INSERT INTO dm.dm_componentitem(id,compid,repositoryid,target,name,summary,xpos,ypos,creatorid,created,modifierid,modified,status,rollup,rollback) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			String propsql = "INSERT INTO dm.dm_compitemprops(compitemid,name,value,encrypted,overridable,appendable) SELECT ?,name,value,encrypted,overridable,appendable FROM dm.dm_compitemprops WHERE compitemid=?";
			String catsql = "INSERT INTO dm.dm_component_categories(id,categoryid) SELECT ?,categoryid FROM dm.dm_component_categories WHERE id=?";
			String updsql = "UPDATE dm.dm_componentitem SET predecessorid=? WHERE id=?";
			PreparedStatement st1 = getDBConnection().prepareStatement(compsql);
			PreparedStatement st2 = getDBConnection().prepareStatement(inssql);
			PreparedStatement st3 = getDBConnection().prepareStatement(propsql);
			// PreparedStatement st4 = m_conn.prepareStatement(dasql);
			PreparedStatement st5 = getDBConnection().prepareStatement(catsql);
			PreparedStatement st6 = getDBConnection().prepareStatement(updsql);
			st1.setInt(1,oldid);
			ResultSet rs1 = st1.executeQuery();
			while (rs1.next()) {
				int id = getID("componentitem");
				st2.setInt(1,id);
				st2.setInt(2,newid);
				int oldcompitemid = rs1.getInt(1);		// id
				// Two way mapping
				ci1map.put(id,oldcompitemid);
				ci2map.put(oldcompitemid,id);
				int repid = getInteger(rs1,2,0);
				if (repid > 0) {
					st2.setInt(3,repid);			// Repository
				} else {
					st2.setNull(3,Type.INT);
				}
				st2.setString(4,rs1.getString(3));		// Target
				st2.setString(5,rs1.getString(4));		// Name
				st2.setString(6,rs1.getString(5));		// Summary
				int predecessorid = getInteger(rs1,6,0);
				System.out.println("putting "+predecessorid+" into hash against oldcompitemid"+oldcompitemid);
				predmap.put(oldcompitemid,predecessorid);
				st2.setInt(7,rs1.getInt(7));			// xpos
				st2.setInt(8,rs1.getInt(8));			// ypos
				st2.setInt(9,rs1.getInt(9));			// creatorid
				st2.setInt(10,rs1.getInt(10));			// creator
				st2.setInt(11,rs1.getInt(11));			// modifierid
				st2.setInt(12,rs1.getInt(12));			// modified
				st2.setString(13,rs1.getString(13));	// Status
				st2.setInt(14,rs1.getInt(14));			// rollup
				st2.setInt(15,rs1.getInt(15));			// rollback
				st2.execute();
				st3.setInt(1,id);
				st3.setInt(2,oldcompitemid);
				st3.execute();
			}
			st2.close();
			st1.close();
			// Now fix the predecessors for each new component item based on the hashmap of the original
			// predecessors
			Iterator<Entry<Integer, Integer>> it = ci1map.entrySet().iterator();
			
			while (it.hasNext()) {
				Map.Entry<Integer,Integer> pair = (Map.Entry<Integer,Integer>)it.next();
				int mapid = pair.getKey();			// original id
				int origid =  pair.getValue();		// new id
				System.out.println("mapid="+mapid+" origid="+origid);
				int predid = predmap.get(origid);
				System.out.println("predid="+predid);
				if (predid>0) {
					int newpredid = ci2map.get(predmap.get(origid));
					System.out.println("Updating compitemid "+mapid+" to have predecessor "+newpredid);
					st6.setInt(1,newpredid);
					st6.setInt(2,mapid);
					st6.execute();
				}
			}
			st6.close();
			//
			// Now set the deployalway, component type and action flags to be the same as the original
			//
			for (int i=0;i<dasql.length;i++) {
				PreparedStatement st4 = getDBConnection().prepareStatement(dasql[i]);
				st4.setInt(1,oldid);
				st4.setInt(2,newid);
				st4.execute();
				st4.close();
			}
			dumpCategories("a1",newid);
			System.out.println(catsql);
			System.out.println("newid="+newid+" oldid="+oldid);
			st5.setInt(1,newid);
			st5.setInt(2,oldid);
			st5.execute();
			st5.close();
			dumpCategories("a2",newid);
			return CopyAttributes("dm.dm_componentvars","compid",oldid,newid);
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
		
	}
	
	public boolean CopyApplicationAttributes(int oldid,int newid)
	{
		try
		{
			String getsql = "SELECT summary,actionid,preactionid,postactionid,successtemplateid,failuretemplateid FROM dm.dm_application WHERE id=?";
			PreparedStatement st1 = getDBConnection().prepareStatement(getsql);
			st1.setInt(1, oldid);
			ResultSet rs1 = st1.executeQuery();
			if (rs1.next()) {
				boolean c=false;
				DynamicQueryBuilder update = new DynamicQueryBuilder(getDBConnection(), "UPDATE dm.dm_application SET ");
				String summary = rs1.getString(1);
				if (!rs1.wasNull()) {
					update.add("summary = ?", summary);
					c=true;
				}
				int actionid = rs1.getInt(2);
				if (!rs1.wasNull()) {
					if (c) update.add(",");
					update.add("actionid = ?",actionid);
					c=true;
				}
				int preactionid = rs1.getInt(3);
				if (!rs1.wasNull()) {
					if (c) update.add(",");
					update.add("preactionid = ?",preactionid);
					c=true;
				}
				int postactionid = rs1.getInt(4);
				if (!rs1.wasNull()) {
					if (c) update.add(",");
					update.add("postactionid = ?",postactionid);
					c=true;
				}
				int successtemplateid = rs1.getInt(5);
				if (!rs1.wasNull()) {
					if (c) update.add(",");
					update.add("successtemplateid = ?",successtemplateid);
					c=true;
				}
				int failuretemplateid = rs1.getInt(6);
				if (!rs1.wasNull()) {
					if (c) update.add(",");
					update.add("failuretemplateid = ?",failuretemplateid);
					c=true;
				}
				if (c) {
					// Update to make
					update.add(" WHERE id=?",newid);
					update.execute();
					update.close();
				}
			}
			// Now we need to copy the components associated with the app.
			String updsql1 = "INSERT INTO dm_applicationcomponent(appid,compid,xpos,ypos)"
			+" SELECT ?,compid,xpos,ypos from dm_applicationcomponent WHERE appid=?";
			System.out.println(updsql1);
			System.out.println("newid="+newid);
			System.out.println("oldid="+oldid);
			PreparedStatement updstmt1 = getDBConnection().prepareStatement(updsql1);
			updstmt1.setInt(1,newid);
			updstmt1.setInt(2,oldid);
			updstmt1.execute();
			updstmt1.close();
			String updsql2 = "INSERT INTO dm_applicationcomponentflows(appid,objfrom,objto)"
			+" SELECT ?,objfrom,objto from dm_applicationcomponentflows WHERE appid=?";
			PreparedStatement updstmt2 = getDBConnection().prepareStatement(updsql2);
			updstmt2.setInt(1,newid);
			updstmt2.setInt(2,oldid);
			updstmt2.execute();
			updstmt2.close();
			// Duplicate the application access
			String updsql3 = "INSERT INTO dm_applicationaccess(appid,usrgrpid,readaccess,writeaccess,viewaccess,updateaccess)"
			+" SELECT ?,usrgrpid,readaccess,writeaccess,viewaccess,updateaccess from dm_applicationaccess WHERE appid=?";
			PreparedStatement updstmt3 = getDBConnection().prepareStatement(updsql3);
			updstmt3.setInt(1,newid);
			updstmt3.setInt(2,oldid);
			updstmt3.execute();
			updstmt3.close();
			// Finally copy all the attributes (variables)
			return CopyAttributes("dm.dm_applicationvars","appid",oldid,newid);
		}
		catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
 public boolean CopyServers(int envid,int domainid)
 {
  System.out.println("Copying servers from env "+getCopyId()+" to new env "+envid);
  Map<Integer,Integer> serverids = new HashMap<Integer, Integer>();
  try
  {
   long t = timeNow();
   String csql = "select count(*) from dm.dm_server where name=? and domainid=?";
   String sql = "SELECT b.name,a.serverid,a.xpos,a.ypos FROM dm.dm_serversinenv a,dm.dm_server b WHERE a.envid=? AND b.id=a.serverid";
   String inssql = "INSERT INTO dm.dm_serversinenv(envid,serverid,xpos,ypos) VALUES(?,?,?,?)";
   String copsql = "INSERT INTO dm.dm_server(id,name,hostname,ownerid,protocol,ogrpid,credid,summary,notes,domainid,basedir,typeid,creatorid,created,modifierid,modified,status) " +
       "SELECT ?,?,hostname,ownerid,protocol,ogrpid,credid,summary,notes,?,basedir,typeid,?,?,?,?,'N' FROM dm.dm_server WHERE id=?";
 
   PreparedStatement st = getDBConnection().prepareStatement(sql);
   
   st.setInt(1,getCopyId());
   ResultSet rs = st.executeQuery();
   while (rs.next()) {
    String newname=rs.getString(1)+" - Copy";
    PreparedStatement cst = getDBConnection().prepareStatement(csql);
    // Check to see if there's already a server with this name in the target domain
    int c=0;
    int n=1;
    do {
    	System.out.println("in loop, checking "+newname);
    	cst.setString(1,newname);
    	cst.setInt(2,domainid);
    	ResultSet crs = cst.executeQuery();
    	if (crs.next()) {
    		System.out.println("got a row");
    		c=crs.getInt(1);
    		System.out.println("c="+c);
    		if (c>0) {
    			n++;
    			newname = rs.getString(1)+" - Copy ("+n+")";
    		}
    	}
    	System.out.println("closing result set");
    	crs.close();
    } while (c>0);
    System.out.println("closing cst");
    cst.close();
    int oldid = rs.getInt(2);
    int newid = getID("Server");
    //
    // Copy all the information from the source server to this new server
    //
    System.out.println("Copying the server details (oldid="+rs.getInt(2)+", newid="+newid);
    PreparedStatement stc = getDBConnection().prepareStatement(copsql);
    stc.setInt(1,newid);
    stc.setString(2,newname);
    stc.setInt(3,domainid);
    stc.setInt(4,getUserID());
    stc.setLong(5,t);
    stc.setInt(6,getUserID());
    stc.setLong(7,t);
    stc.setInt(8,oldid);
    stc.execute();
    stc.close();
    //
    // Now map this new server to the new environment
    //
    PreparedStatement st2 = getDBConnection().prepareStatement(inssql);
    st2.setInt(1,envid);
    st2.setInt(2,newid);
    st2.setInt(3,rs.getInt(3));
    st2.setInt(4,rs.getInt(4));
    st2.execute();
    st2.close();
    //
    // Copy the component mapping
    //
    CopyServerAttributes(oldid,newid);
    
    System.out.println("INSERT INTO dm.dm_serversinenv(envid,serverid,xpos,ypos) VALUES("+envid+","+newid+","+rs.getInt(2)+","+rs.getInt(3)+")");
    //
    // Record the server ids - old and new values into hashmap
    //
    serverids.put(oldid,newid);
   }
   rs.close();
   st.close();
   //
   // Get the connections between the servers
   //
   PreparedStatement st4 = getDBConnection().prepareStatement("SELECT serverfrom,serverfromedge,serverto,servertoedge,label,style FROM dm.dm_server_connections WHERE envid=?");
   PreparedStatement st5 = getDBConnection().prepareStatement("INSERT INTO dm.dm_server_connections(envid,serverfrom,serverfromedge,serverto,servertoedge,label,style) VALUES(?,?,?,?,?,?,?)");
   st4.setInt(1,getCopyId());
   ResultSet rs4 = st4.executeQuery();
   while(rs4.next()) {
    st5.setInt(1,envid);
    st5.setInt(2,serverids.get(rs4.getInt(1)));
    st5.setInt(3,rs4.getInt(2));
    st5.setInt(4,serverids.get(rs4.getInt(3)));
    st5.setInt(5,rs4.getInt(4));
    st5.setString(6,rs4.getString(5));
    st5.setString(7,rs4.getString(6));
    st5.execute();
   }
   rs4.close();
   st4.close();
   st5.close();
   return true;
  }
  catch(SQLException e) {
   e.printStackTrace();
   rollback();
  }
  return false;
 }

 
 	public String VerifyCompTargetDomain(int compid,int tgtdomain)
 	{
 		Hashtable<Integer,String> domlist = new Hashtable<Integer,String>();
 		Component comp = getComponent(compid,true);
 		System.out.println("Domain List:");
 		domlist.put(tgtdomain,"Y");
 		Domain dom = getDomain(tgtdomain);
 		do {
 			domlist.put(dom.getId(),"Y");
 			dom = dom.getDomain();	// go to parent domain
 		} while (dom != null);
 		Action preAction = comp.getPreAction();
 		Action postAction = comp.getPostAction();

 		if (preAction != null && domlist.containsKey(preAction.getDomainId()) == false) {
 			return "Action "+preAction.getName();
 		}
 		if (postAction != null && domlist.containsKey(postAction.getDomainId()) == false) {
 			return "Action "+postAction.getName();
 		}
 		List<ComponentItem> items = getComponentItems(compid);
 		for (ComponentItem i: items) {
 			Repository repos = i.getRepository();
 			if (repos != null && domlist.containsKey(repos.getDomainId()) == false) {
 				return "Repository "+repos.getName();
 			}
 		}
 		int typeid = comp.getComptypeId();
 		// Check if this component type is declared in one of the target domain's ancestors
 		String getsql="SELECT domainid,name FROM dm.dm_type WHERE id=?";
 		try {
 			PreparedStatement st = getDBConnection().prepareStatement(getsql);
 			st.setInt(1,typeid);
 			ResultSet rs = st.executeQuery();
 			if (rs.next()) {
 				if (domlist.containsKey(rs.getInt(1)) == false) {
 					String name = rs.getString(2);
 					rs.close();
 					st.close();
 					return "Component Type "+name;
 				}
 			}
 			rs.close();
 			st.close();
 		} catch (SQLException e) {
 			e.printStackTrace();
			rollback();
			return "Failed checking component type";
 		}
 		return null;
 	}
 	
 	public String VerifyAppTargetDomain(int appid,int tgtdomain)
 	{
 		//
 		// Finds the domain heirachy of the specified app and checks that it will "fit"
 		// in the specified target domain. It does this by ensuring that none of the
 		// objects referenced from the specified application are outside of the new
 		// domain heirarchy
 		//
 		Hashtable<Integer,String> domlist = new Hashtable<Integer,String>();
 		Application app = getApplication(appid,true);
 		Domain dom = getDomain(tgtdomain);
 		do {
 			domlist.put(dom.getId(),"Y");
 			dom = dom.getDomain();	// go to parent domain
 		} while (dom != null);
 		Action customAction = app.getCustomAction();
 		Action preAction = app.getPreAction();
 		Action postAction = app.getPostAction();
 		if (customAction != null && domlist.containsKey(customAction.getDomainId()) == false) {
 			return "Action "+customAction.getName();
 		}
 		if (preAction != null && domlist.containsKey(preAction.getDomainId()) == false) {
 			return "Action "+preAction.getName();
 		}
 		if (postAction != null && domlist.containsKey(postAction.getDomainId()) == false) {
 			return "Action "+postAction.getName();
 		}
 		NotifyTemplate successTemplate = app.getSuccessTemplate();
 		NotifyTemplate failureTemplate = app.getFailureTemplate();
 		if (successTemplate != null && domlist.containsKey(successTemplate.getDomainId()) == false) {
 			return "Template "+successTemplate.getName();
 		}
 		if (failureTemplate != null && domlist.containsKey(failureTemplate.getDomainId()) == false) {
 			return "Template "+failureTemplate.getName();
 		}
 		// Check Components
 		List<Component> comp = getComponents(ObjectType.APPLICATION, appid, false);
 		for (Component x : comp) {
 			if (domlist.containsKey(x.getDomainId()) == false) return "Component "+x.getName();
 		}
 		return null;
 	}
 	
	public boolean CopyTemplate(int oldid,int notifierid,int newid,String newname)
	{
		try
		{
			long t=timeNow();
			String copysql1 = "INSERT INTO dm.dm_template(id,name,summary,notifierid,creatorid,modifierid,created,modified,status,subject,body) SELECT ?,?,summary,?,?,?,?,?,status,subject,body FROM dm.dm_template WHERE id=?";
			String copysql2 = "INSERT INTO dm.dm_templaterecipients(templateid,usrgrpid,userid,ownertype) SELECT ?,usrgrpid,userid,ownertype FROM dm.dm_templaterecipients WHERE templateid=?";
			PreparedStatement st1 = getDBConnection().prepareStatement(copysql1);
			st1.setInt(1,newid);
			st1.setString(2,newname);
			st1.setInt(3,notifierid);
			st1.setInt(4,getUserID());
			st1.setInt(5,getUserID());
			st1.setLong(6,t);
			st1.setLong(7,t);
			st1.setInt(8,oldid);
			st1.execute();
			st1.close();
			PreparedStatement st2 = getDBConnection().prepareStatement(copysql2);
			st2.setInt(1,newid);
			st2.setInt(2,oldid);
			st2.execute();
			st2.close();
			return true;
		}
		catch(SQLException e) {
			e.printStackTrace();
			rollback();
		}
		return false;
	}
	
	public String GetPasteName(String Type,int domainid,String oldname)
	{
		boolean Exists=true;
		try
		{
			if (Type.equalsIgnoreCase("lifecycle")) Type="domain";
			if (Type.equalsIgnoreCase("appversion")) Type="application";
			if (Type.equalsIgnoreCase("procedure")) Type="action";
			if (Type.equalsIgnoreCase("function")) Type="action";
			if (Type.equalsIgnoreCase("builder")) Type="buildengine";
			String TestName;
			if (Type.equalsIgnoreCase("action"))
			{
				// Actions/Procedures/Functions cannot contain spaces
				TestName=oldname+"_Copy";
			} else {
				TestName=oldname+" - Copy";
			}
			int n=2;
			String sql1 = "SELECT id,name FROM dm.dm_"+Type+" where name=? AND domainid=?";
			String sql2 = "SELECT a.id,a.name FROM dm.dm_template a,dm.dm_notify b where a.name=? AND a.notifierid=b.id AND b.domainid=?";
			PreparedStatement st = getDBConnection().prepareStatement(Type.equalsIgnoreCase("template")?sql2:sql1);
			System.out.println("SELECT id,name FROM dm.dm_"+Type+" where name='"+TestName+"' AND domainid="+domainid);
			while (Exists) {
				st.setString(1,TestName);
				st.setInt(2,domainid);
				System.out.println("Testing TestName="+TestName+" against domainid "+domainid+" (table dm.dm_"+Type+")");
				ResultSet rs = st.executeQuery();
				if (rs.next()) {
					System.out.println("Already exists..");
					// An object with this name already exists in this domain
					if (Type.equalsIgnoreCase("action")) {
						TestName=oldname+"_Copy"+n;
					} else {
						TestName=oldname+" - Copy ("+n+")";
					}
					System.out.println("TestName now "+TestName);
					n++;
				} else {
					Exists=false;
				}
				rs.close();
			}
			st.close();
			System.out.println("Returning "+TestName);
			return TestName;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return oldname+" Copy";
	}
	
	public void CommitPaste(boolean commit)
	{
		System.out.println("CommitPaste("+commit+")");
		if (commit) {
			try
			{
				getDBConnection().commit();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				rollback();
			}
		}
	}
	
	public void SetPasteError(String errtext)
	{
		m_PasteError = errtext;
	}
	
	public String GetPasteError()
	{
		return m_PasteError;
	}
	
	public boolean CopyAction(int newid,String newname,int domainid,int origid)
	{
		System.out.println("CopyAction(\""+newname+"\", domainid="+domainid+" origid="+origid);
		try {
			long t = timeNow();
			String sql1="INSERT INTO dm.dm_action(id,name,filepath,summary,domainid,function, "
					+ "graphical,ownerid,creatorid,created,modifierid,modified,"
					+ "ogrpid,status,copy,kind,categoryid,interpreter) "
					+ "SELECT ?,?,filepath,summary,?,function, "
					+ "graphical,ownerid,?,?,?,?, "
					+ "ogrpid,status,copy,kind,categoryid,interpreter "
					+ "FROM dm.dm_action WHERE id=?";
			String childsql[] = {
			"INSERT INTO dm.dm_actionfrags(actionid,windowid,xpos,ypos,typeid,title,summary,parentwindowid) "
					+ "SELECT ?,windowid,xpos,ypos,typeid,title,summary,parentwindowid FROM dm.dm_actionfrags WHERE actionid=?",
			"INSERT INTO dm.dm_actionflows(actionid,flowid,nodefrom,nodeto,pos) "
					+ "SELECT ?,flowid,nodefrom,nodeto,pos FROM dm.dm_actionflows WHERE actionid=?",
			"INSERT INTO dm.dm_actionfragattrs(actionid,windowid,attrid,value) "
					+ "SELECT ?,windowid,attrid,value FROM dm.dm_actionfragattrs WHERE actionid=?",				
			"INSERT INTO dm.dm_actionarg(actionid,name,type,outpos,required,switch,pad,inpos,switchmode,negswitch) "
					+ "SELECT ?,name,type,outpos,required,switch,pad,inpos,switchmode,negswitch FROM dm.dm_actionarg WHERE actionid=?",
			"INSERT INTO dm.dm_actionaccess(actionid,usrgrpid,readaccess,writeaccess,viewaccess,updateaccess) "
					+ "SELECT ?,usrgrpid,readaccess,writeaccess,viewaccess,updateaccess FROM dm.dm_actionaccess WHERE actionid=?",
			"INSERT INTO dm.dm_action_categories(id,categoryid) "
					+ "SELECT ?,categoryid FROM dm.dm_action_categories WHERE id=?"
			};
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,newid);
			stmt1.setString(2,newname);
			stmt1.setInt(3,domainid);
			stmt1.setInt(4,getUserID());
			stmt1.setLong(5,t);
			stmt1.setInt(6,getUserID());
			stmt1.setLong(7,t);
			stmt1.setInt(8,origid);
			stmt1.execute();
			if (stmt1.getUpdateCount() >0) {
				for (int i=0;i<childsql.length;i++) {
					PreparedStatement cstmt = getDBConnection().prepareStatement(childsql[i]);
					cstmt.setInt(1,newid);
					cstmt.setInt(2,origid);
					cstmt.execute();
					cstmt.close();
				}
			}
			stmt1.close();
		} catch(SQLException e) {
			e.printStackTrace();
			rollback();
			return false;
		}
		return true;
	}
	
	public boolean CopyFunction(int newid,String newname,int domainid,int origid)
	{
		System.out.println("CopyFunction(\""+newname+"\", domainid="+domainid+" origid="+origid);
		try {
			int newtextid=0;
			boolean isFunction=false;
			long t = timeNow();
			String sql1="SELECT textid,function FROM dm.dm_action WHERE id=?";
			String sql2="INSERT INTO dm.dm_actiontext(id,data) SELECT ?,data FROM dm.dm_actiontext WHERE id=?";
			String sql3="INSERT INTO dm.dm_action(id,name,filepath,summary,domainid,textid,repositoryid, "
					+ "function,pluginid,resultisexpr,remote,graphical,ownerid,creatorid,created, "
					+ "modifierid,modified,ogrpid,status,copy,kind,categoryid,interpreter) "
					+ "SELECT ?,?,filepath,summary,?,?,repositoryid, "
					+ "function,pluginid,resultisexpr,remote,graphical,ownerid,?,?, "
					+ "?,?,ogrpid,status,copy,kind,categoryid,interpreter "
					+ "FROM dm.dm_action WHERE id=?";
			String sql4="INSERT INTO dm.dm_actionaccess(actionid,usrgrpid,readaccess,writeaccess,viewaccess,updateaccess) "
					+ "SELECT ?,usrgrpid,readaccess,writeaccess,viewaccess,updateaccess FROM dm.dm_actionaccess WHERE actionid=?";
			String sql5="INSERT INTO dm.dm_actionarg(actionid,name,type,outpos,required,switch,pad,inpos,switchmode,negswitch) "
					+ "SELECT ?,name,type,outpos,required,switch,pad,inpos,switchmode,negswitch FROM dm.dm_actionarg WHERE actionid=?";
			String sql6="INSERT INTO dm.dm_action_categories(id,categoryid) SELECT ?,categoryid FROM dm.dm_action_categories WHERE id=?";
			String sql7="INSERT INTO dm.dm_fragments(id,name,summary,categoryid,exitpoints,creatorid,created,modifierid,modified,drilldown,actionid,functionid) SELECT ?,?,summary,categoryid,exitpoints,?,?,?,?,drilldown,?,? FROM dm.dm_fragments WHERE ? in (actionid,functionid)";
			String sql8="INSERT INTO dm.dm_fragment_categories(id,categoryid) SELECT ?,categoryid FROM dm.dm_action_categories WHERE id=?";	// deliberate - take category from action for fragment
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,origid);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				int textid = getInteger(rs1,1,0);
				isFunction = getBoolean(rs1,2,false);
				if (textid>0) {
					// This is a procedure or function with DMScript stored in the database
					// Duplicate the DMScript text body.
					newtextid = getID("actiontext");
					PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
					stmt2.setInt(1,newtextid);
					stmt2.setInt(2,textid);
					stmt2.execute();
					stmt2.close();
				}
			}
			rs1.close();
			stmt1.close();
			PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
			stmt3.setInt(1,newid);
			stmt3.setString(2,newname);
			stmt3.setInt(3,domainid);
			if (newtextid>0) {
				stmt3.setInt(4,newtextid);
			} else {
				stmt3.setNull(4,Types.INTEGER);
			}
			stmt3.setInt(5,getUserID());
			stmt3.setLong(6,t);
			stmt3.setInt(7,getUserID());
			stmt3.setLong(8,t);
			stmt3.setInt(9,origid);
			stmt3.execute();
			stmt3.close();
			// Duplicate the access control
			PreparedStatement stmt4 = getDBConnection().prepareStatement(sql4);
			stmt4.setInt(1,newid);
			stmt4.setInt(2,origid);
			stmt4.execute();
			stmt4.close();
			// Now duplicate all the arguments
			PreparedStatement stmt5 = getDBConnection().prepareStatement(sql5);
			stmt5.setInt(1,newid);
			stmt5.setInt(2,origid);
			stmt5.execute();
			stmt5.close();
			// Now set the new function/procedure into the appropriate categories
			PreparedStatement stmt6 = getDBConnection().prepareStatement(sql6);
			stmt6.setInt(1,newid);
			stmt6.setInt(2,origid);
			stmt6.execute();
			stmt6.close();
			// Now create the matching fragment
			PreparedStatement stmt7 = getDBConnection().prepareStatement(sql7);
			int newfragid = this.getID("fragments");
			stmt7.setInt(1,newfragid);
			stmt7.setString(2,newname);
			stmt7.setInt(3,getUserID());
			stmt7.setLong(4,t);
			stmt7.setInt(5,getUserID());
			stmt7.setLong(6,t);
			if (isFunction) {
				stmt7.setNull(7,Types.INTEGER);		// Action ID
				stmt7.setInt(8,newid);				// Function ID
			} else {
				stmt7.setInt(7,newid);				// Action ID
				stmt7.setNull(8, Types.INTEGER);	// Function ID
			}
			stmt7.setInt(9,origid);
			stmt7.execute();
			// Now add the fragment to the fragment category
			PreparedStatement stmt8 = getDBConnection().prepareStatement(sql8);
			stmt8.setInt(1,newfragid);
			stmt8.setInt(2,origid);
			stmt8.execute();
			stmt8.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
			rollback();
			return false;
		}
		return true;
	}
	
	public void CopyComponentVersions(Component oldComp,Component newComp)
	{
		// On a paste operation, copies all the child versions, giving each of them the same "new name" as the parent.
		String copysql="INSERT INTO dm.dm_component(id,name,domainid,summary,ownerid,creatorid,created,modifierid,modified,parentid,"
				+ "predecessorid,xpos,ypos,ogrpid,preactionid,postactionid,actionid,status,rollup,rollback,filteritems,deployalways,basedir,"
				+ "comptypeid,branch) "
				+ "SELECT ?,?,?,summary,ownerid,?,?,?,?,?,"
				+ "?,xpos,ypos,ogrpid,preactionid,postactionid,actionid,status,rollup,rollback,filteritems,deployalways,basedir,"
				+ "comptypeid,branch "
				+ "FROM dm.dm_component WHERE id=?";
		String oldName = oldComp.getName();
		String newName = newComp.getName();		// On a paste operation this should be something like xxx - Copy;n
		System.out.println("oldName=["+oldName+"]");
		System.out.println("newName=["+newName+"]");
		System.out.println("newName.lastIndexOf('-')="+newName.lastIndexOf('-'));
		String postFix = newName.substring(newName.lastIndexOf('-'));
		System.out.println("postFix="+postFix);
		long t = timeNow();
		try {
			PreparedStatement st = getDBConnection().prepareStatement(copysql);
			List<Component> children = oldComp.getVersions();			
		
			System.out.println("children.size()="+children.size());
			HashMap<Integer, Integer> idmap = new HashMap<Integer, Integer>(children.size()+1);
			int n = getID("component");
			// Create a mapping table from old id to new id
			System.out.println("Mapping "+oldComp.getId()+" to "+newComp.getId());
			idmap.put(oldComp.getId(), newComp.getId());
			for (Component c: children)
			{
				System.out.println("Mapping "+c.getId()+" to "+n);
				idmap.put(c.getId(),n);
				n++;
			}
			for (Component cc: children) {
				System.out.println("Copy child component "+cc.getName());
				st.setInt(1,idmap.get(cc.getId()));
				st.setString(2,cc.getName()+postFix);
				st.setInt(3,newComp.getDomainId());
				st.setInt(4,getUserID());
				st.setLong(5,t);
				st.setInt(6,getUserID());
				st.setLong(7,t);
				st.setInt(8,newComp.getId());
				int pi = cc.getPredecessorId();
				System.out.println("predecessorid="+pi);
				if (pi>0) st.setInt(9,idmap.get(pi));
				else st.setNull(9,Types.INTEGER);
				st.setInt(10,cc.getId());
				st.execute();
				CopyComponentAttributes(cc.getId(),idmap.get(cc.getId()));
			}
			setID("component",n);
		} catch (SQLException e) {
			e.printStackTrace();
			rollback();
		}
	}
	
	public void dumpCategories(String p,int id) {
		// debug - print out categories
		System.out.println("dumpCategories ("+p+") id="+id);
		try {
			String sql = "select id,categoryid from dm.dm_component_categories where id="+id;
			Statement stmt = getDBConnection().createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				System.out.println("id="+rs.getInt(1)+", categoryid="+rs.getInt(2));
			}
			rs.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	public DMObject PasteObject(int domainid,int parentid,int xpos,int ypos,boolean pie,boolean commit)
	{
		DMObject ret = null;
		ObjectType oot = null;
		Category cat = null;	// for objects in categories
		System.out.println("PasteObject m_copyobjtype="+getCopyObjType()+" domainid="+domainid+" parentid="+parentid+" commit="+commit);
		if (getCopyObjType().equalsIgnoreCase("environment")) {
			parentid=domainid;
			Environment env = getEnvironment(getCopyId(),false);
			ret = env;
		}
		else
		if (getCopyObjType().equalsIgnoreCase("server")) {
			Server server = getServer(getCopyId(),false);
			ret = server;
		}
		else
		if (getCopyObjType().equalsIgnoreCase("builder")) {
			Builder builder = getBuilder(getCopyId());
			ret = builder;
		}
		else
		if (getCopyObjType().equalsIgnoreCase("template")) {
			NotifyTemplate template = getTemplate(getCopyId());
			ret = template;
		}
		else
		if (getCopyObjType().equalsIgnoreCase("appversion") || getCopyObjType().equalsIgnoreCase("application")) {
			Application app = getApplication(getCopyId(),false);
			ret = app;
		}
		else
		if (getCopyObjType().equalsIgnoreCase("component") || getCopyObjType().equalsIgnoreCase("compversion")) {
			Component component = getComponent(getCopyId(),true);
			setCopyObjType("component");
			cat = component.getCategory();
			System.out.println("in PasteObject, type is component cat is "+cat);
			if (cat != null) System.out.println("cat name is "+cat.getName());
			ret = component;
		}
		else
		if (	getCopyObjType().equalsIgnoreCase("procedure") || 
				getCopyObjType().equalsIgnoreCase("function")  ||
				getCopyObjType().equalsIgnoreCase("action")) {
			Action action = getAction(getCopyId(),false);
			cat = action.getCategory();
			if (action.getKind() != ActionKind.GRAPHICAL) {
				// Not an action, must be procedure or function
				if (action.isFunction()) oot = ObjectType.FUNCTION;
				else oot = ObjectType.PROCEDURE;
			}
			ret = action;
		}
		int newid = getID(getCopyObjType());
		if (ret != null) {
			String errtext = null;
			String newname=GetPasteName(getCopyObjType(),domainid,ret.getName());		
			ret.setName(newname);
			ret.setId(newid);
			System.out.println("ret.getObjectType()="+ret.getObjectType());
			switch((oot != null)?oot:ret.getObjectType()) {
			case ENVIRONMENT:
				CreateNewObject(getCopyObjType(),newname,domainid,parentid,newid,xpos,ypos,"",commit);
				// Create copies of the servers
				System.out.println("Copying servers");
				if (CopyServers(newid,domainid)) {
					if (CopyAttributes("dm.dm_environmentvars","envid",getCopyId(),newid)) {
						CommitPaste(commit);
					}
				}
				break;
			case COMPONENT:
			case COMPVERSION:
				//
				// Pasting a component version into a domain automatically makes it a BASE version
				//
				errtext = VerifyCompTargetDomain(getCopyId(),domainid);
				if (errtext == null) {
					CreateNewObject(getCopyObjType(),newname,domainid,parentid,newid,xpos,ypos,"",commit);
					Component oldComp = getComponent(getCopyId(),true);
					Component newComp = getComponent(newid,true);
					// CopyComponentAttributes duplicates the category entries.
					//if (cat != null) {
					//	System.out.println("Adding new component "+newid+" to category "+cat.getId());
					//	addToCategory(cat.getId(), newComp.getOtid());
					//}
					System.out.println("About to CopyComponentAttributes");
					dumpCategories("a",newid);
					if (CopyComponentAttributes(getCopyId(),newid)) {
						dumpCategories("b",newid);
						// If the source component was a BASE version, copy all the versions
						System.out.println("CopyComponentAttributes returns, parentid="+oldComp.getParentId());
						if (oldComp.getParentId()==0) {
							System.out.println("Copying all versions...");
							CopyComponentVersions(oldComp,newComp);
						}
						dumpCategories("c",newid);
						CommitPaste(commit);
					}
				} else {
					//
					// Cannot paste the app into this domain - at least one of its dependents (components,
					// templates, action etc) lies outside the target domain
					//
					SetPasteError(errtext);
					ret = null;
				}
				break;
			case BUILDER:
				System.out.println("Pasting BUILDER");
				CreateNewObject("buildengine",newname,domainid,parentid,newid,xpos,ypos,"builders",commit);
				System.out.println("Calling CopyBuilderAttributes, m_copyid="+getCopyId()+" newid="+newid);
				if (CopyBuilderAttributes(getCopyId(),newid)) CommitPaste(commit);
				break;
			case SERVER:
				System.out.println("Pasting SERVER pie="+pie);
				String treeid=pie?"environments":"servers";
				CreateNewObject(getCopyObjType(),newname,domainid,parentid,newid,xpos,ypos,treeid,commit);
				System.out.println("Calling CopyServerAttributes, m_copyid="+getCopyId()+" newid="+newid);
				if (CopyServerAttributes(getCopyId(),newid)) CommitPaste(commit);
				break;
			case TEMPLATE:
				if (CopyTemplate(getCopyId(),parentid,newid,newname)) CommitPaste(commit);
				break;
			case APPLICATION:
			case APPVERSION:
				System.out.println("Creating new object of type "+getCopyObjType()+" newname="+newname+" domainid="+domainid+" parentid="+parentid);
				//
				// Pasting an application version into a domain automatically makes it a BASE version
				//
				errtext = VerifyAppTargetDomain(getCopyId(),domainid);
				if (errtext == null) {
					ObjectTypeAndId otid = CreateNewObject("application",newname,domainid,0,newid,xpos,ypos,"",commit);
					System.out.println("otid id="+otid.getId());
					System.out.println("otid object type = "+otid.getObjectType());
					System.out.println("newid="+newid);
					System.out.println("m_copyid="+getCopyId());
					if (CopyApplicationAttributes(getCopyId(),newid)) CommitPaste(commit);
				} else {
					//
					// Cannot paste the app into this domain - at least one of its dependents (components,
					// templates, action etc) lies outside the target domain
					//
					SetPasteError(errtext);
					ret = null;
				}
				break;
			case PROCEDURE:
			case FUNCTION:
				System.out.println("Creating new function/procedure of type "+getCopyObjType()+" newname="+newname+" domainid="+domainid);
				if (cat != null) {
					Action newAction = getAction(newid,true);
					addToCategory(cat.getId(),newAction.getOtid());
				}
				if (CopyFunction(newid,newname,domainid,getCopyId())) CommitPaste(commit);
				break;
			case ACTION:
				System.out.println("Creating new action newname="+newname+" domainid="+domainid);
				/*
				 *  CopyAction copies the category
				if (cat != null) {
					Action newAction = getAction(newid,true);
					addToCategory(cat.getId(),newAction.getOtid());
				}
				*/
				if (CopyAction(newid,newname,domainid,getCopyId())) CommitPaste(commit);
				break;
			default:
				break;
			}
		} else {
			System.out.println("m_copyobjtype="+getCopyObjType()+" not yet supported");
		}
		
		return ret;
	}
	
	public DMObject PasteObject(int domainid,int parentid,int xpos,int ypos,boolean commit)
	{
		return PasteObject(domainid,parentid,xpos,ypos,false,commit);
	}
	
	public DMObject PasteObject(int domainid,int parentid,int xpos,int ypos)
	{
		return PasteObject(domainid,parentid,xpos,ypos,true);
	}
	
	public DMObject PasteObject(int domainid,int parentid)
	{
		return PasteObject(domainid,parentid,-1,-1,true);
	}
	
	public DMObject PasteObject(int domainid,int parentid,boolean commit)
	{
		return PasteObject(domainid,parentid,-1,-1,commit);
	}
	
	public void ArchiveAction(Action action)
	{
		//
		// Called every time a procedure/function or action is modified in such a way as
		// to change its operation. When called, this function checks to see if the action
		// has ever been used during a deployment (by checking in dm_deploymentactions). If
		// it HAS then the action is "archived" by ...
		// a) copying it to the same domain with a new name and a parentid of the original action
		// b) the new action id replaces the original action id in dm_deploymentactions. That
		//    way, the deployment links to the original action used and we can click on it and
		//    go to the archived version.
		//
		boolean needsArchiving=false;
		String sql1="SELECT count(*) FROM dm.dm_deploymentactions WHERE actionid=?";
		try {
			int origid = action.getId();
			int domainid = action.getDomainId();
			PreparedStatement stmt1 = getDBConnection().prepareStatement(sql1);
			stmt1.setInt(1,origid);
			ResultSet rs1 = stmt1.executeQuery();
			if (rs1.next()) {
				int c = rs1.getInt(1);
				if (c>0) needsArchiving=true;
			}
			rs1.close();
			stmt1.close();
			if (needsArchiving) {
				// This action/procedure/function has been used in a deployment
				// Take a copy, mark it as archived and update dm_deploymentactions
				int newid = this.getID("action");
				String newname = action.getName()+"_Archived";
				//
				// Check to see if this name already exists. If so, append a number and try again
				//
				String sql2 = "SELECT count(*) FROM dm.dm_action WHERE name=? AND domainid=?";
				PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
				stmt2.setInt(2,domainid);
				int cv=0;
				int n=1;
				do {
					stmt2.setString(1,newname);
					ResultSet rs2 = stmt2.executeQuery();
					if (rs2.next()) {
						cv = rs2.getInt(1);
					}
					rs2.close();
					if (cv>0) newname = action.getName()+"_Archived_"+n;
					n++;
				} while (cv>0);
				stmt2.close();
				if (action.getKind() == ActionKind.GRAPHICAL) {
					CopyAction(newid, newname, domainid, origid);
				} else {
					CopyFunction(newid, newname, domainid, origid);
				}
				//
				// Set the parent and archived flags for the newly created action/procedure/function
				//
				String sql3 = "UPDATE dm.dm_action SET parentid=?, status='A' WHERE id=?";
				PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
				stmt3.setInt(1,origid);
				stmt3.setInt(2,newid);
				stmt3.execute();
				stmt3.close();
				//
				// Now update the action referenced in the deploymentactions table to point to the
				// archived action
				//
				String sql4 = "UPDATE dm.dm_deploymentactions SET actionid=? WHERE actionid=?";
				PreparedStatement stmt4 = getDBConnection().prepareStatement(sql4);
				stmt4.setInt(1,newid);
				stmt4.setInt(2,origid);
				stmt4.execute();
				stmt4.close();
				//
				// Finally, put an entry in the history table so we know when we were archived
				//
				String updsql="INSERT INTO dm.dm_historynote(objid,kind,\"WHEN\",note,userid,id) VAlUES(?,?,?,?,?,?)";
				long t = timeNow();
				PreparedStatement updstmt = getDBConnection().prepareStatement(updsql);
				//
				// Original (archived) Action
				// --------------------------
				//
				int hnid = getID("HistoryNote");
				updstmt.setInt(1,newid);
				updstmt.setInt(2,action.getObjectType().value());
				updstmt.setLong(3,t);
				updstmt.setString(4,"Archived due to Modification");
				updstmt.setInt(5,getUserID());
				updstmt.setInt(6,hnid);
				updstmt.execute();
				//
				// New (Modified) Action
				// ---------------------
				//
				hnid = getID("HistoryNote");
				updstmt.setInt(1,origid);
				updstmt.setInt(2,action.getObjectType().value());
				updstmt.setLong(3,t);
				updstmt.setString(4,"Created Archived Version "+newname);
				updstmt.setInt(5,getUserID());
				updstmt.setInt(6,hnid);
				updstmt.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
			rollback();
		}
	}
	
	//
	// Search
	//
	/*
	public List<SearchResult> SearchForObjects(String ss)
	{
		List<SearchResult> res = new ArrayList<SearchResult>();
		String iconnames[] = {
			"user-large.png"	
		};
		String sql[] = {
				"SELECT id,name,realname FROM dm.dm_user WHERE status='N' AND domainid in ("+m_domainlist+") AND name=? "
		};
		try
		{
			for (int i=0;i<sql.length;i++) {
				PreparedStatement stmt = m_conn.prepareStatement(sql[i]);
				stmt.setString(1,ss);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					SearchResult x = new SearchResult();
					x.setId(rs.getInt(1));
					x.setName(rs.getString(2));
					x.setSummary(rs.getString(3));
					x.setIconName(iconnames[i]);
					res.add(x);
				}
				rs.close();
				stmt.close();
			}
			return res;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			rollback();
		}
		return null;
	}
	*/
	
	public void rollback()
	{
		try {
		 if (getDBConnection() != null)
			  getDBConnection().rollback();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void DeleteProperties(ComponentItem componentItem)
	{
	 try {
	  String dsql = "DELETE FROM dm.dm_compitemprops WHERE compitemid = ?";
	  PreparedStatement stmt = getDBConnection().prepareStatement(dsql);
	  stmt.setInt(1, componentItem.getId());
	  stmt.execute();
	  stmt.close();

	 } catch (SQLException e) {
	  e.printStackTrace();
	 }
	}

 public JSONArray getFileAuditReport(String loc)
 {
  String sql = "Select a.deploymentid As Deployment, b.startts As DeployedAt, e.name As userName, c.name As Application, d.name As Environment, a.componentname As Component, a.servername As Server, a.targetfilename As File From dm.dm_deploymentxfer a, dm.dm_deployment b, dm.dm_application c, dm.dm_environment d, dm.dm_user e Where a.deploymentid = b.deploymentid And c.id = b.appid And d.id = b.envid And e.id = b.userid And a.deploymentid In (Select Max(a.deploymentid) From dm.dm_deploymentxfer a, dm.dm_deployment b, dm.dm_application c, dm.dm_environment d Where a.deploymentid = b.deploymentid And c.id = b.appid And d.id = b.envid   And a.checksum2 = ? Group By c.name,   d.name,   a.componentname,   a.servername) And a.checksum2 = ? Order By 1 Desc, 5, 7"; 
  JSONArray ret = new JSONArray();
  
  try {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   stmt.setString(1, loc);
   stmt.setString(2, loc);
   ResultSet rs = stmt.executeQuery();

   while (rs.next())
   {
    JSONObject obj = null;
    
    obj = new JSONObject().add("deployment", rs.getInt(1))
      .add("deployedat",  (rs.getTimestamp(2) == null) ? "" : rs.getTimestamp(2).toString())
      .add("username", rs.getString(3))
      .add("application", rs.getString(4))
      .add("environment", rs.getString(5))      
      .add("component", rs.getString(6))
      .add("server", rs.getString(7))      
      .add("file", rs.getString(8));
      
    ret.add(obj);
   }
   rs.close();
   stmt.close();
   return ret;
  } catch (SQLException e) {
   e.printStackTrace();
  } 
  return ret;
 }
 
 public JSONArray getSuccess4EnvReport()
 {
  String sql = "Select dm.dm_deployment.deploymentid As Deployment, dm.dm_deployment.exitcode As Exit, dm.dm_deployment.exitstatus As Message, dm.dm_environment.name As Environment, dm.dm_application.name As Application, dm.dm_deployment.startts As deploytimestamp, dm.dm_deploymentxfer.componentname As Component, dm.dm_deploymentxfer.servername As Server, dm.dm_deploymentxfer.targetfilename As File, dm.dm_deploymentxfer.reponame As Repository, dm.dm_user.name As username From dm.dm_deployment, dm.dm_deploymentxfer, dm.dm_environment, dm.dm_user, dm.dm_application Where dm.dm_deployment.deploymentid = dm.dm_deploymentxfer.deploymentid And dm.dm_deployment.envid = dm.dm_environment.id And dm.dm_deployment.userid = dm.dm_user.id And dm.dm_application.id = dm.dm_deployment.appid Order By Deployment Desc, Server, File";
  JSONArray ret = new JSONArray();
  
  try {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   ResultSet rs = stmt.executeQuery();

   while (rs.next())
   {
    JSONObject obj = null;
    
    obj = new JSONObject().add("deployment", rs.getInt(1))
      .add("exit", (rs.getInt(2) == 0) ? "Success" : "Failed")
      .add("message", rs.getString(3))
      .add("environment", rs.getString(4))
      .add("application", rs.getString(5))      
      .add("deploytimestamp", (rs.getTimestamp(6) == null) ? "" : rs.getTimestamp(6).toString())
      .add("component", rs.getString(7))     
      .add("server", rs.getString(8))       
      .add("file", rs.getString(9))
      .add("repository", rs.getString(10))
      .add("username", rs.getString(11));
    
    ret.add(obj);
   }
   rs.close();
   stmt.close();
   return ret;
  } catch (SQLException e) {
   e.printStackTrace();
  } 
  return ret;
 }
 
 public JSONArray getServerAuditReport()
 {
  String sql = "Select 'Identical' as Result, dm.dm_discovery.servername, dm.dm_discovery.targetfilename, TIMESTAMP 'epoch' + dm.dm_discovery.discovery_time * INTERVAL '1 second' discovery_time From dm.dm_discovery where dm.dm_discovery.detectedmd5 = dm.dm_discovery.deployedmd5 union Select 'Changed', dm.dm_discovery.servername, dm.dm_discovery.targetfilename, TIMESTAMP 'epoch' + dm.dm_discovery.discovery_time * INTERVAL '1 second' discovery_time From dm.dm_discovery where dm.dm_discovery.detectedmd5 <> dm.dm_discovery.deployedmd5 order by 2,3,1"; 
  JSONArray ret = new JSONArray();
  
  try {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   ResultSet rs = stmt.executeQuery();

   while (rs.next())
   {
    JSONObject obj = null;
    
    obj = new JSONObject().add("result", rs.getString(1)).add("servername",rs.getString(2)).add("targetfilename", rs.getString(3)).add("discovery_time",  (rs.getTimestamp(4) == null) ? "" : rs.getTimestamp(4).toString());
  
    ret.add(obj);
   }
   rs.close();
   stmt.close();
   return ret;
  } catch (SQLException e) {
   e.printStackTrace();
  } 
  return ret;
 }
 
 public JSONArray getServerInventoryReport()
 {
  String sql = "Select a.deploymentid As Deployment, b.startts As DeployedAt, e.name As userName, c.name As Application, d.name As Environment, a.componentname As Component, a.servername As Server, a.targetfilename As File From dm.dm_deploymentxfer a, dm.dm_deployment b, dm.dm_application c, dm.dm_environment d, dm.dm_user e Where a.deploymentid = b.deploymentid And c.id = b.appid And d.id = b.envid And e.id = b.userid And a.deploymentid In (Select Max(a.deploymentid) From dm.dm_deploymentxfer a, dm.dm_deployment b, dm.dm_application c, dm.dm_environment d Where a.deploymentid = b.deploymentid And c.id = b.appid And d.id = b.envid  Group By c.name,   d.name,   a.componentname,   a.servername) Order By 1 desc,7,8 asc,6"; 
  JSONArray ret = new JSONArray();
  
  try {
   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
   ResultSet rs = stmt.executeQuery();

   while (rs.next())
   {
    JSONObject obj = null;
    
    obj = new JSONObject().add("deployment", rs.getInt(1))
      .add("deploytimestamp",  (rs.getTimestamp(2) == null) ? "" : rs.getTimestamp(2).toString())
      .add("username", rs.getString(3))
      .add("application", rs.getString(4))
      .add("environment", rs.getString(5))      
      .add("component", rs.getString(6))
      .add("server", rs.getString(7))      
      .add("file", rs.getString(8));
      
    ret.add(obj);
   }
   rs.close();
   stmt.close();
   return ret;
  } catch (SQLException e) {
   e.printStackTrace();
  } 
  return ret;
 }
 
 public String getDMHome()
 {
	 return m_context.getInitParameter("DMHOME");
 }
 
 private boolean ValidateArguments(int actionid,Element root) throws Exception
 {
	 boolean res=false;
	 System.out.println("ValidateArguments for orig id="+actionid);
	 NodeList arglist = root.getElementsByTagName("arguments");
	 if (arglist.getLength()==0) arglist = root.getElementsByTagName("cmdline");
	 if (arglist.getLength()==0) throw(new Exception("no arguments/cmdline attribute found"));
	 if (arglist.getLength()!=1) throw(new Exception("multiple arguments attributes found"));
	 Element arguments = (Element)arglist.item(0);
	 NodeList args = arguments.getElementsByTagName("argument");
	 // Check the argument count is the same
	 String csql="select count(*) from dm.dm_actionarg where actionid=? and type in ('entry','checkbox')";
	 PreparedStatement cstmt = getDBConnection().prepareStatement(csql);
	 cstmt.setInt(1,actionid);
	 ResultSet crs = cstmt.executeQuery();
	 if (crs.next()) {
		 // got the count - is the same as the number of arguments in the XML file?
		 System.out.println("Original argument count= "+crs.getInt(1));
		 System.out.println("Arguments in XML file  = "+args.getLength());
		 res = (crs.getInt(1) != args.getLength()); // res = true if argument count mismatch
	 }
	 crs.close();
	 cstmt.close();
	 System.out.println("about to loop res="+res);
	 for (int i=0;!res && i<args.getLength();i++) {
		 // Loop will not enter is res is set (argument count mismatch)
		 Node nNode = args.item(i);
		 if (nNode.getNodeType() != Node.ELEMENT_NODE) continue;
		 Element nElement = (Element)nNode;
		 String argname = nElement.getAttribute("name");
		 String argtype = nElement.getAttribute("type");
		 String reqdtext = nElement.getAttribute("required");
		 if (reqdtext.equalsIgnoreCase("false")) reqdtext="N";
		 else
		 if (reqdtext.equalsIgnoreCase("true")) reqdtext="Y";
		 //
		 // Check what's in the DB for this argument name. If everything matches, we're good to continue. If
		 // anything differs, bail out and set the return flag to indicate that this is a new interface and
		 // we need to rename the original action.
		 //
		 String sql = "SELECT count(*) FROM dm.dm_actionarg WHERE actionid=? AND name=? AND type=? AND required=?";
		 PreparedStatement stmt = getDBConnection().prepareStatement(sql);
		 stmt.setInt(1,actionid);
		 stmt.setString(2,argname);
		 stmt.setString(3,argtype);
		 stmt.setString(4,reqdtext);
		 System.out.println("select count(*) from dm.dm_actionarg WHERE actionid="+actionid+" and name='"+argname+"' and type='"+argtype+"' and required='"+reqdtext+"'");
		 ResultSet rs = stmt.executeQuery();
		 if (rs.next()) {
			 // We should always get one row - what's the count?
			 int c = rs.getInt(1);
			 System.out.println("name=["+argname+"] c="+c);
			 if (c==0) {
				 // This argument did not match what's currently in the DB. Need to rename original action
				 System.out.println("argument type mismatch - bailing");
				 res=true;
			 }
		 }
	 }
	 System.out.println("ValidateArguments returning "+res);
	 return res;
 }
 
 private void RenameFragment(int actionid)
 {
	 String selsql="SELECT id,name FROM dm.dm_fragments WHERE ? IN (actionid,functionid)";
	 String updsql="UPDATE dm.dm_fragments set name=? WHERE id=?";
	 try {
		 PreparedStatement selstmt = getDBConnection().prepareStatement(selsql);
		 selstmt.setInt(1,actionid);
		 ResultSet rs = selstmt.executeQuery();
		 if (rs.next()) {
			 // Fragment exists linked to this action. Rename it.
			 String chksql="SELECT count(*) from dm.dm_fragments WHERE name=?";
			 PreparedStatement chkstmt = getDBConnection().prepareStatement(chksql);
			 int id = rs.getInt(1);
			 String origname = rs.getString(2);
			 boolean retry=true;
			 int lv=1;
			 String newName=origname;
			 while (retry) {
				 newName = origname+"_orig_"+lv;
				 chkstmt.setString(1,newName);
				 ResultSet chkrs = chkstmt.executeQuery();
				 if (chkrs.next()) {
					 if (chkrs.getInt(1)==0) retry=false;	// This name is unique
				 }
				 chkrs.close();
				 lv++;
			 }
			 chkstmt.close();
			 //
			 // newName is the new name for the fragment
			 //
			 PreparedStatement updstmt = getDBConnection().prepareStatement(updsql);
			 updstmt.setString(1,newName);
			 updstmt.setInt(2,id);
			 updstmt.execute();
			 updstmt.close();
		 }
		 rs.close();
		 selstmt.close();
	 } catch(SQLException e) {
		 e.printStackTrace();
	 }
 }
 
 private void RenameAction(int actionid,int domainid,String origname)
 {
	 String selsql="SELECT count(*) FROM dm.dm_action WHERE name=? AND domainid=?";
	 String updsql="UPDATE dm.dm_action set name=? WHERE id=?";
	 try {
		 PreparedStatement selstmt = getDBConnection().prepareStatement(selsql);
		 selstmt.setInt(2,domainid);
		 boolean retry=true;
		 int lv=1;
		 String newName=origname;
		 while (retry) {
			 newName = origname+"_orig_"+lv;
			 selstmt.setString(1,newName);
			 ResultSet selrs = selstmt.executeQuery();
			 if (selrs.next()) {
				 if (selrs.getInt(1)==0) retry=false;	// This name is unique
			 }
			 selrs.close();
			 lv++;
		 }
		 selstmt.close();
		 //
		 // Update the name
		 //
		 PreparedStatement updstmt = getDBConnection().prepareStatement(updsql);
		 updstmt.setString(1,newName);
		 updstmt.setInt(2,actionid);
		 updstmt.execute();
		 //
		 // Now change any mapped fragment name
		 //
		 RenameFragment(actionid);
	 } catch(SQLException e) {
		 e.printStackTrace();
	 }
	
 }
	 
 public JSONObject ImportFunction(int domainid,String filepath)
 {
	 JSONObject res = new JSONObject();
	 File inputFile = new File(filepath);
	 try {
		 DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		 DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		 Document doc = dBuilder.parse(inputFile);		// Or can be String in which case it's a URI
		 doc.getDocumentElement().normalize();
		 Element root = doc.getDocumentElement();
		 String RootNodeName = root.getNodeName();
		 if (!(RootNodeName.equals("action") || RootNodeName.equals("function"))) {
			 throw(new Exception("RootNode should be \"action\" or \"function\""));
		 }
		 boolean isFunction=(RootNodeName.equalsIgnoreCase("function"));
		 String actionname = root.getAttribute("name");
		 if (actionname == null || actionname.length()==0) throw(new Exception("No action name"));
		 
		 
		 String summary = root.getAttribute("summary");
		 String grph = root.getAttribute("isGraphical");
		 boolean isGraphical = (grph != null)?grph.equalsIgnoreCase("Y"):false;
		 String category = root.getAttribute("category");
		 // Category may not actually be set
		 int categoryid=0;
		 if (category != null && category.length() > 0) {
			 // Look up the category id based on the category name
			 PreparedStatement stc = getDBConnection().prepareStatement("SELECT id FROM dm.dm_category WHERE name=?");
			 stc.setString(1,category);
			 ResultSet rsc = stc.executeQuery();
			 if (rsc.next()) {
				 categoryid = rsc.getInt(1);
			 }
			 rsc.close();
			 stc.close();
			 if (categoryid==0) {
				 // No such category yet exists - create it
				 categoryid = getID("category");
				 PreparedStatement ci = getDBConnection().prepareStatement("INSERT INTO dm.dm_category(id,name) VALUES(?,?)");
				 ci.setInt(1,categoryid);
				 ci.setString(2,category);
				 ci.execute();
				 if (ci.getUpdateCount()==0) {
					 throw(new Exception("Failed to insert row into category table"));
				 }
				 ci.close();
			 }
		 }
		 NodeList kindlist = root.getElementsByTagName("kind");
		 if (kindlist.getLength()==0) throw(new Exception("no kind attribute found"));
		 if (kindlist.getLength()!=1) throw(new Exception("multiple kind attributes found"));
		 Element kind = (Element)kindlist.item(0);
		 int nKind = Integer.parseInt(kind.getTextContent());
		 //
		 // Check to see if an action with this name already exists
		 //
		 String actsql = "SELECT id,kind,function,graphical,textid FROM dm.dm_action WHERE name=? AND domainid=?";
		 PreparedStatement astmt = getDBConnection().prepareStatement(actsql);
		 astmt.setString(1,actionname);
		 astmt.setInt(2,domainid);
		 ResultSet ars = astmt.executeQuery();
		 boolean alreadyExists=false;
		 int origId=0;
		 int origTextId=0;
		 if (ars.next()) {
			 System.out.println("Matching name already in this domain");
			 // A procedure/function or action with this name already exists in this domain.
			 // What we do now depends on the kind.
			 // if the "kind" is the same and it's a procedure/function/graphical like the existing
			 // action then we'll check to see if this action has been referenced anywhere. If not,
			 // then we'll simply overwrite the existing action. If it HAS been referenced anywhere
			 // then we'll create a new action with a new name (to avoid breaking existing flows).
			 // If the kind differs or the old one was a function and this is a procedure (or something)
			 // then we'll throw an error. User can always amend the XML if required.
			 //
			 origId = ars.getInt(1);
			 int origKind = ars.getInt(2);
			 boolean origfunc = getBoolean(ars,3,false);
			 boolean origgraphical = getBoolean(ars,4,false);
			 origTextId = getInteger(ars,5,0);
			 String origtype = "Action";
			 if (!origgraphical) {
				 origtype=(origfunc)?"function":"procedure";
			 }
			 if (isGraphical != origgraphical || origfunc != isFunction) throw(new Exception("Cannot import - "+origtype+" with the same name already exists in domain"));
			 if (origKind != nKind) throw(new Exception("Cannot import - "+origtype+" kinds differ"));
			 //
			 // Okay, if we get here then the imported procedure/function as the same name as an existing
			 // function/procedure in the domain but the types and kind are the same. Let's mark that we
			 // need to check the parameters.
			 //
			 alreadyExists=true;
		 }
		 ars.close();
		 astmt.close();
		 
		 NodeList interpreterlist = root.getElementsByTagName("interpreter");
		 String interpreter = null;
		 if (interpreterlist.getLength()==1) {
			 // There is an interpreter tag
			 Element interp = (Element)interpreterlist.item(0);
			 interpreter = interp.getTextContent();
		 }
		 if (interpreterlist.getLength()>1) throw(new Exception("multiple kind attributes found"));
		 if (nKind<0 || nKind>6) throw(new Exception("Invalid kind value"));
		 
		 int actionid;
		 
		 if (alreadyExists) {
			 boolean changeOrigName = ValidateArguments(origId,root);
			 if (changeOrigName) {
				 //
				 // Argument types differ - we only need to rename the original if the
				 // original action is being used somewhere.
				 //
				 System.out.println("changeOrigName is true");
				 RenameAction(origId,domainid,actionname);
				 actionid = getID("Action");
			 } else {
				 // Argument types are the same - keep the same interface but change
				 // command line/DMScript or back-end script
				 actionid = origId;
			 }
		 } else {
			 // New function/procedure
			 actionid = getID("Action");
		 }
		 String otid = (isFunction?"fn":"pr")+actionid+"-"+nKind;
		 
		 long t = timeNow();
		 String sql=null;
		 switch(nKind) {
		 case 1:
			 // DMSCRIPT in repository
			 break;
		 case 2:
			 // DMSCRIPT in database
			 sql = 	"INSERT INTO dm.dm_action( 							" +
					"id,name,kind,summary,domainid,ownerid,				" +
					"creatorid,created,modifierid,modified,status,		" +
					"textid,function,resultisexpr,graphical,categoryid)	" +
					"VALUES( ?,?,?,?,?,?,	" +
					"		 ?,?,?,?,?,		" +
					"		 ?,?,?,?,?)		";
			 int textid;
			 if (actionid != origId) {
				 textid = getID("actiontext");
			 } else {
				 textid = origTextId;
			 }
			 PreparedStatement st1 = getDBConnection().prepareStatement(sql);
			 st1.setInt(1,actionid);		// id
			 st1.setString(2,actionname);	// name
			 st1.setInt(3,nKind);			// kind
			 st1.setString(4,summary);		// summary
			 st1.setInt(5,domainid);		// domainid
			 st1.setInt(6,getUserID());		// ownerid
			 st1.setInt(7,getUserID());		// creatorid
			 st1.setLong(8,t);				// created
			 st1.setInt(9,getUserID());		// modifierid
			 st1.setLong(10,t);				// modified
			 st1.setString(11, "N");		// status
			 st1.setInt(12,textid);
			 st1.setString(13,isFunction?"Y":"N");
			 if (isFunction) {
				 // This is a function - is the result a DMScript expression?
				 String expr = root.getAttribute("isExpr");
				 if (expr == null) expr="N";
				 st1.setString(14,expr);
			 } else {
				 // Not a function - resultisexpr is NULL
				 st1.setNull(14,Types.VARCHAR);
			 }
			 st1.setString(15,"N");		// Graphical (N)
			 st1.setInt(16,categoryid);
			 // Need to insert text before we insert the record into dm_action.
			 String sinsql;
			 if (actionid == origId) {
				 // Changing existing DMScript with same interface.
				 System.out.println("Updating dm.dm_actiontext (actionid == origId)");
				 sinsql="UPDATE dm.dm_actiontext SET data=? WHERE id=?";
			 } else {
				 System.out.println("Inserting into dm.dm_actiontext (actionid != origId)");
				 sinsql="INSERT INTO dm.dm_actiontext(data,id) VALUES(?,?)";
			 }
			 NodeList dmscriptlist = root.getElementsByTagName("dmscript");
			 if (kindlist.getLength()==0) throw(new Exception("no dmscript attribute found"));
			 if (kindlist.getLength()!=1) throw(new Exception("multiple dmscript attributes found"));
			 Element eDMscript = (Element)dmscriptlist.item(0);
			 String dmscript = eDMscript.getTextContent();	// BASE64 encoded DMScript
			 byte[] decodedBytes = Base64.decodeBase64(dmscript);
			 String decodedScript = new String(decodedBytes);
			 PreparedStatement sin = getDBConnection().prepareStatement(sinsql);
			 System.out.println("decodedScript");
			 System.out.println("-------------");
			 System.out.println(decodedScript);
			 sin.setString(1,decodedScript);
			 sin.setInt(2,textid);
			 System.out.println("textid="+textid);
			 boolean v = sin.execute();
			 System.out.println("Execute of "+sinsql+" returns "+v+" (update count="+sin.getUpdateCount()+")");
			 sin.close();
			 if (actionid != origId) {
				 // now do the insert into dm_action
				 st1.execute();
				 st1.close();
				 addToCategory(categoryid, new ObjectTypeAndId(ObjectType.ACTION,actionid));
				 
				 /*
	    		 st1 = m_conn.prepareStatement("INSERT INTO dm.dm_action_categories values (?,?)");
	    		 st1.setInt(1,actionid);
	    		 st1.setInt(2,categoryid);
	    		 st1.execute();
	    		 */
				 //
				 // Now do the arguments
				 //
				 NodeList arglist = root.getElementsByTagName("arguments");
				 if (arglist.getLength()==0) throw(new Exception("no arguments attribute found"));
				 if (arglist.getLength()!=1) throw(new Exception("multiple arguments attributes found"));
				 Element arguments = (Element)arglist.item(0);
				 NodeList args = arguments.getElementsByTagName("argument");
				 for (int i=0;i<args.getLength();i++) {
					 Node nNode = args.item(i);
					 if (nNode.getNodeType() != Node.ELEMENT_NODE) continue;
					 Element nElement = (Element)nNode;
					 String argname = nElement.getAttribute("name");
					 String argtype = nElement.getAttribute("type");
					 String inpos = nElement.getAttribute("inpos");
					 String switchmode = nElement.getAttribute("switchmode");
					 String switchname = nElement.getAttribute("switch");
					 String negswitch = nElement.getAttribute("negswitch");
					 String padtext = nElement.getAttribute("pad");
					 String pad = (padtext!=null)?padtext.equalsIgnoreCase("true")?"Y":"N":"N";
					 String reqdtext = nElement.getAttribute("required");
					 String reqd = (reqdtext!=null)?reqdtext.equalsIgnoreCase("true")?"Y":"N":"N";
					 String inssql="INSERT INTO dm.dm_actionarg(actionid,name,type,required,switch,negswitch,pad,switchmode,inpos) "+
					 "VALUES(?,?,?,?,?,?,?,?,?)";
					 PreparedStatement aa = getDBConnection().prepareStatement(inssql);
					 aa.setInt(1,actionid);
					 aa.setString(2,argname);
					 aa.setString(3,argtype);
					 aa.setString(4,reqd);
					 aa.setString(5,switchname);
					 aa.setString(6,negswitch);
					 aa.setString(7,pad);
					 aa.setString(8,switchmode);
					 aa.setInt(9,Integer.parseInt(inpos));
					 aa.execute();
				 }
			 }
			 st1.close();
			 break;
		 case 3:
		 case 4:
			 // LOCAL or REMOTE script
			 sql = 	"INSERT INTO dm.dm_action( 												" +
					"id,name,kind,summary,domainid,ownerid,									" +
					"creatorid,created,modifierid,modified,status,							" +
					"filepath,function,resultisexpr,graphical,copy,categoryid,interpreter) 	" +
					"VALUES( ?,?,?,?,?,?,	" +
					"		 ?,?,?,?,?,		" +
					"		 ?,?,?,?,?,?,?)		";
			 NodeList cmdlist = root.getElementsByTagName("cmdline");
			 if (cmdlist.getLength()==0) throw(new Exception("no cmdline attribute found"));
			 if (cmdlist.getLength()!=1) throw(new Exception("multiple cmdline attributes found"));
			 Element cmdline = (Element)cmdlist.item(0);
			 String scriptname = null;
			 NodeList attlist = cmdline.getElementsByTagName("*");
			 int outpos=1;
			 for (int i=0;i<attlist.getLength();i++) {
				 Node nNode = attlist.item(i);
				 String nodename = nNode.getNodeName();
				 if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					 Element nElement = (Element)nNode;
					 if (nodename.equalsIgnoreCase("script")) {
						 if (scriptname != null) throw(new Exception("multiple scriptname attributes found"));
						 scriptname = nElement.getAttribute("name");
						 //
						 // Okay, we should be good to start inserting data
						 //
						 PreparedStatement st11 = getDBConnection().prepareStatement(sql);
						 st11.setInt(1,actionid);		// id
						 st11.setString(2,actionname);	// name
						 st11.setInt(3,nKind);			// kind
						 st11.setString(4,summary);		// summary
						 st11.setInt(5,domainid);		// domainid
						 st11.setInt(6,getUserID());		// ownerid
						 st11.setInt(7,getUserID());		// creatorid
						 st11.setLong(8,t);				// created
						 st11.setInt(9,getUserID());		// modifierid
						 st11.setLong(10,t);				// modified
						 st11.setString(11, "N");		// status
						 st11.setString(12,scriptname);
						 st11.setString(13,isFunction?"Y":"N");
						 if (isFunction) {
							 // This is a function - is the result a DMScript expression?
							 String expr = root.getAttribute("isExpr");
							 if (expr == null) expr="N";
							 st11.setString(14,expr);
						 } else {
							 // Not a function - resultisexpr is NULL
							 st11.setNull(14,Types.VARCHAR);
						 }
						 st11.setString(15,"N");		// Graphical (N)
						 if (nKind == 4) {
							 // Copy is only significant for REMOTE scripts
							 String copy = kind.getAttribute("copy");
							 if (copy == null) copy="N";
							 st11.setString(16,copy);
						 } else {
							 // For LOCAL scripts, set COPY to null
							 st11.setNull(16,Types.VARCHAR);
						 }
						 st11.setInt(17,categoryid);
						 st11.setString(18,interpreter);			 
						 // Got everything necessary for our "script" - insert the row into dm_action
						 if (actionid != origId) {
							 // Only insert the row if the action is new.
							 st11.execute();
							 st11.close();
							 addToCategory(categoryid, new ObjectTypeAndId(ObjectType.ACTION,actionid));
							 /*
						     st11 = m_conn.prepareStatement("INSERT INTO dm.dm_action_categories values (?,?)");
						     st11.setInt(1,actionid);
						     st11.setInt(2,categoryid);
						     st11.execute();
						     */
						 } else {
							 // We're updating the original action. We know that the arguments must be
							 // the same (we've already checked this). But the flags and the command line
							 // (and, indeed, the backend script) may be different. So let's delete all the
							 // command line flags and let the import recreate them.
							 //
							 String delsql="DELETE FROM dm.dm_actionarg WHERE actionid=? AND type='false'";
							 PreparedStatement delstmt = getDBConnection().prepareStatement(delsql);
							 delstmt.setInt(1,actionid);
							 delstmt.execute();
						 }
						 st11.close();
					 }
					 else
					 if (nodename.equalsIgnoreCase("flag")) {
						 if (actionid==0) throw(new Exception("flag/argument before script tag"));
						 String flagname = nElement.getAttribute("name");
						 String switchname = nElement.getAttribute("switch");
						 String padtext = nElement.getAttribute("pad");
						 String pad = (padtext!=null)?padtext.equalsIgnoreCase("true")?"Y":"N":"N";
//						 System.out.println("flag=["+flagname+"] switchname=["+switchname+"] pad=["+pad+"]");
						 // Insert into dm_actionarg
						 String inssql="INSERT INTO dm.dm_actionarg(actionid,name,type,required,switch,pad,switchmode,outpos) "+
						 "VALUES(?,?,?,?,?,?,?,?)";
						 PreparedStatement aa = getDBConnection().prepareStatement(inssql);
						 aa.setInt(1,actionid);
						 aa.setString(2,flagname);
						 aa.setString(3,"false");
						 aa.setString(4,"N");
						 aa.setString(5,switchname);
						 aa.setString(6,pad);
						 aa.setString(7,"A");
						 aa.setInt(8,outpos);
						 aa.execute();
						 outpos++;
					 }
					 else
					 if (nodename.equalsIgnoreCase("argument")) {
						 if (actionid != origId) {
							 // Only insert argument if this is a new action
							 String argname = nElement.getAttribute("name");
							 String argtype = nElement.getAttribute("type");
							 String inpos = nElement.getAttribute("inpos");
							 String switchmode = nElement.getAttribute("switchmode");
							 String switchname = nElement.getAttribute("switch");
							 String negswitch = nElement.getAttribute("negswitch");
							 String padtext = nElement.getAttribute("pad");
							 String pad = (padtext!=null)?padtext.equalsIgnoreCase("true")?"Y":"N":"N";
							 String reqdtext = nElement.getAttribute("required");
							 String reqd = (reqdtext!=null)?reqdtext.equalsIgnoreCase("true")?"Y":"N":"N";
							 String inssql="INSERT INTO dm.dm_actionarg(actionid,name,type,required,switch,negswitch,pad,switchmode,inpos,outpos) "+
							 "VALUES(?,?,?,?,?,?,?,?,?,?)";
							 PreparedStatement aa = getDBConnection().prepareStatement(inssql);
							 aa.setInt(1,actionid);
							 aa.setString(2,argname);
							 aa.setString(3,argtype);
							 aa.setString(4,reqd);
							 aa.setString(5,switchname);
							 aa.setString(6,negswitch);
							 aa.setString(7,pad);
							 aa.setString(8,switchmode);
							 aa.setInt(9,Integer.parseInt(inpos));
							 aa.setInt(10,outpos);
							 aa.execute();
							 outpos++;
						 }
					 }
					 else throw(new Exception("Unknown type \""+nodename+"\""));
				 }
			 }
			 
			 // Okay, if we've got here without any problems then the DB is all inserted. Tell
			 // the engine to create the script in its scripts directory.
			 // NOTE: We need to check if we really need to create a script. For Ansible, we may
			 // just be creating a command line...
			 //
			 Domain domain = getDomain(domainid);
			 Engine engine = (domain != null) ? domain.findNearestEngine() : null;
			 if (engine == null) {
				 throw(new Exception("Could not locate engine for domain"));
			 }
			 CommandLine m_cmd = engine.doCreateScript();
			 System.out.println("Reading file "+filepath);
			 FileInputStream fis=new FileInputStream(filepath);
			 String scriptBody = "";
			 byte[] b = new byte[4096];
			 while (fis.read(b)>0) {
				 scriptBody += new String(b);
			 }
			 fis.close();
			 System.out.println("************************************");
			 System.out.println(scriptBody);
			 System.out.println("************************************");
			 try
			 {
			  m_cmd.run(true, scriptBody + "\n", true);
			 }
			 catch (Exception e)
			 {
			  System.err.println(e.getMessage());
			 }
			 break;
		 case 5:
			 // PLUGIN (will this ever be exported?)
			 break;
		 case 6:
			 // GRAPHICAL flow
			 break;
		 }
		 // Is there a fragment? Fragments are optional
		 NodeList fraglist = root.getElementsByTagName("fragment");
		 if (fraglist.getLength()>1) throw(new Exception("multiple fragment attributes found"));
		 if (fraglist.getLength()==1) {
			 // There is a fragment
			 Element fragment = (Element)fraglist.item(0);
			 String fragname = fragment.getAttribute("name");
			 String fragsumm = fragment.getAttribute("summary");
//			 System.out.println("fragment name=["+fragname+"] summary=["+fragsumm+"]");
			 String fraginssql = "INSERT INTO dm.dm_fragments(id,name,summary,categoryid,exitpoints,"+
			 "creatorid,created,modifierid,modified,drilldown,actionid,functionid) "+
			 "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
			 PreparedStatement fragins = getDBConnection().prepareStatement(fraginssql);
			 int fragid = getID("fragments");
			 fragins.setInt(1,fragid);
			 fragins.setString(2,fragname);
			 fragins.setString(3,fragsumm);
			 fragins.setInt(4,categoryid);
			 fragins.setInt(5,1);
			 fragins.setInt(6,getUserID());
			 fragins.setLong(7,t);
			 fragins.setInt(8,getUserID());
			 fragins.setLong(9,t);
			 fragins.setString(10,"N");	// No drilldown
			 if (isFunction) {
				 fragins.setNull(11,Types.INTEGER);		// actionid
				 fragins.setInt(12,actionid);			// functionid
			 } else {
				 fragins.setInt(11,actionid);			// actionid
				 fragins.setNull(12,Types.INTEGER);		// functionid
			 }
			 fragins.execute();
			 fragins.close();
			 /*
			 fragins = m_conn.prepareStatement("INSERT INTO dm.dm_fragment_categories values (?,?)");
			 fragins.setInt(1,fragid);
			 fragins.setInt(2,categoryid);
			 fragins.execute();
			 fragins.close();
			 */
			 addToCategory(categoryid,new ObjectTypeAndId(ObjectType.FRAGMENT,fragid));
    
			 // Now the fragment parameters (if any)
			 String fragattins="INSERT INTO dm.dm_fragmentattrs("
			 			+ "id,typeid,attype,atname,tablename,inherit,atorder,required, default_value)	"
			 			+ "VALUES(?,?,?,?,?,?,?,?,?)";
			 PreparedStatement fai = getDBConnection().prepareStatement(fragattins);
			 NodeList plist = fragment.getElementsByTagName("parameter");
			 for (int p=0;p<plist.getLength();p++) {
				 String tablename = null;
				 String inherit = null;
				 Node param = plist.item(p);
				 if (param.getNodeType() == Node.ELEMENT_NODE) {
					 Element eParam = (Element)param;
					 NodeList tablist = eParam.getElementsByTagName("tablename");
					 if (tablist.getLength()==1) {
						 // There is a table record associated with this parameter
						 Element eTable = (Element)tablist.item(0);
						 tablename = eTable.getTextContent();
						 inherit = eTable.getAttribute("inherit");
						 if (inherit == null) inherit="N";
					 }
					 String paramname = eParam.getAttribute("name");
					 String paramtype = eParam.getAttribute("type");
					 String reqd = eParam.getAttribute("required");
      String default_value_param = eParam.getAttribute("default_value");
      String default_value = (default_value_param == null) ? "" : default_value_param;
//					 System.out.println("paramname=["+paramname+"] paramtype=["+paramtype+"] reqd=["+reqd+"]");
					 int fragintid=getID("fragmentattrs");
					 fai.setInt(1,fragintid);
					 fai.setInt(2,fragid);
					 fai.setString(3,paramtype);
					 fai.setString(4,paramname);
					 if (tablename != null) {
						 fai.setString(5,tablename);
						 fai.setString(6,inherit);
					 } else {
						 // No table - both tablename and inherit should be NULL
						 fai.setNull(5,Types.VARCHAR);
						 fai.setNull(6,Types.VARCHAR);
					 }
					 fai.setInt(7,p+1);
					 fai.setString(8,reqd);
					 fai.setString(9, default_value);
					 fai.execute();
				 }
			 }
		 }
		 System.out.println("COMMIT");
		 getDBConnection().commit();
		 res.add("result",true);
		 res.add("otid",otid);
	 } catch (Exception e) {
		 e.printStackTrace();
		 System.out.println("message is : "+e.getMessage());
		 res.add("result",false);
		 res.add("error",e.getMessage());
		 System.out.println("res is "+res);
		 rollback();
	 }
	 return res;
 }
 
 public void ExportFunction(int actionid,PrintWriter out)
 {
	 // Exports the specified function as an XML file
	 String sql = 	"SELECT 			a.name,	"
			+		"					a.filepath,	"
	 		+		"					a.summary,	"
			+		"					a.domainid,	"
	 		+		"					d.name,		"
	 		+		"					at.data,	"
	 		+		"					a.function,	"
	 		+		"					a.resultisexpr,	"
	 		+		"					a.graphical,	"
	 		+		"					a.copy,	"
	 		+		"					a.kind,	"
	 		+		"					f.categoryid,	"
	 		+		"					a.interpreter,	"
	 		+		"					a.repositoryid	"	
	 		+		"FROM				dm.dm_action	a	"
	 		+ 		"LEFT JOIN			dm.dm_domain d ON d.id = a.domainid	"
	 		+		"LEFT OUTER JOIN	dm.dm_actiontext at ON at.id = a.textid "
	 		+ 		"LEFT OUTER JOIN	dm.dm_fragments f ON a.id in (f.actionid,f.functionid) "
	 		+ 		"WHERE				a.id=?";
	 try {
		   PreparedStatement stmt = getDBConnection().prepareStatement(sql);
		   stmt.setInt(1,actionid);
		   ResultSet rs = stmt.executeQuery();
		   while (rs.next()) {
			   // got the action
			   String name = getString(rs,1,"");
			   String filepath = getString(rs,2,"");
			   String summary = getString(rs,3,"");
			   int domainid = getInteger(rs,4,0);
			   String DMScriptData = getString(rs,6,null);
			   boolean isFunction = getBoolean(rs,7,false);
			   boolean isExpr = getBoolean(rs,8,false);
			   boolean isGraphical = getBoolean(rs,9,false);
			   boolean isCopy = getBoolean(rs,10,false);
			   int kind = rs.getInt(11);
			   int categoryid = getInteger(rs,12,0);
			   String category="";
			   if (categoryid>0) {
				   PreparedStatement c = getDBConnection().prepareStatement("SELECT name FROM dm.dm_category WHERE id=?");
				   c.setInt(1, categoryid);
				   ResultSet cr = c.executeQuery();
				   if (cr.next()) {
					   category=cr.getString(1);
				   }
				   cr.close();
				   c.close();
			   }
			   String interpreter = rs.getString(13);
			   int repository = getInteger(rs,14,0);

			   if (isFunction) {
				   out.println("<function name=\""+name+"\" summary=\""+summary+"\" isExpr=\""+(isExpr?"Y":"N")+"\" category=\""+category+"\">");
			   } else {
				   out.println("<action name=\""+name+"\" summary=\""+summary+"\" isGraphical=\""+(isGraphical?"Y":"N")+"\" category=\""+category+"\">");
			   }
			   out.println("<kind copy=\""+(isCopy?"Y":"N")+"\">"+kind+"</kind>");
			   if (interpreter != null) {
				   out.println("<interpreter>"+interpreter+"</interpreter>");
			   }
			   if (kind == 2) {
				   if (DMScriptData != null) {
					   // DMScript stored in database
					   out.println("<dmscript>");
					   byte[] encodedBytes = Base64.encodeBase64(DMScriptData.getBytes());
					   out.println(new String(encodedBytes));
					   out.println("</dmscript>");
				   } else {
					   // DMScript stored in repository
					   out.println("<dmscript>");
					   out.println("In Repository "+repository);
					   out.println("</dmscript>");
				   }
			   }
			   else if (kind == 4) {
				   // External Script - need to get this from engine.
				   Domain d = getDomain(domainid);
				   if (d!=null) {
					   Engine engine = d.findNearestEngine();
					   String res = engine.dumpScript(actionid);
					   out.println(res);
				   }
			   }
			   if (kind == 3 || kind == 4) {
				   // LOCAL and REMOTE scripts have a cmdline
				   out.println("<cmdline>");
				   out.println("<script name=\""+filepath+"\" />");
			   } else {
				   out.println("<arguments>");
			   }
			   if (kind>=1 && kind<=5) {
				   // Arguments for everything other than Graphical Flows
				   String sql4 = 	"	SELECT	name,		"
						   +		"			type,		"
						   +		"			required,	"
						   +		"			switch,		"
						   +		"			pad,		"
						   +		"			inpos,		"
						   +		"			switchmode,	"
						   +		"			negswitch	"
						   +		"	FROM	dm.dm_actionarg	"
						   +		"	WHERE	actionid=?	"
						   +		"	ORDER BY	outpos";
				   PreparedStatement stmt4 = getDBConnection().prepareStatement(sql4);
				   stmt4.setInt(1,actionid);
				   ResultSet rs4 = stmt4.executeQuery();
				   while (rs4.next()) {
					   String argname = getString(rs4,1,"");
					   String argtype = getString(rs4,2,"");
					   boolean reqd = getBoolean(rs4,3,false);
					   String switchtext = getString(rs4,4,"");
					   boolean pad = getBoolean(rs4,5,false);
					   int inpos = getInteger(rs4,6,0);
					   String switchmode = getString(rs4,7,"");
					   String negswitch = getString(rs4,8,"");
					   if (switchmode.equalsIgnoreCase("A")) {
						   out.println("<flag name=\""+argname+"\" switch=\""+switchtext+"\" pad=\""+pad+"\" />");
					   } else {
						   out.println("<argument name=\""+argname+"\" type=\""+argtype+"\" inpos=\""+inpos+"\" switchmode=\""+switchmode+"\" switch=\""+switchtext+"\" negswitch=\""+negswitch+"\" pad=\""+pad+"\" required=\""+reqd+"\" />");
					   }
				   }
			   }
			   if (kind == 3 || kind == 4) {
				   out.println("</cmdline>");
			   } else {
				   out.println("</arguments>");
			   }
			   //
			   // Check if there is a fragment wrapping this action/function
			   //
			   String sql2 = 	"	SELECT	id,			"
					   	+		"			name,		"
					   	+		"			summary		"
					   	+		"	FROM	dm.dm_fragments	"
					   	+		"	WHERE	? in (actionid,functionid)";
			   PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
			   stmt2.setInt(1,actionid);
			   ResultSet rs2 = stmt2.executeQuery();
			   while (rs2.next()) {
				   int typeid = rs2.getInt(1);
				   String fname = getString(rs2,2,"");
				   String fsumm = getString(rs2,3,"");
				   out.println("<fragment name=\""+fname+"\" summary=\""+fsumm+"\">");
				   //
				   // Now get the fragment attributes
				   //
				   String sql3 = 	"	SELECT	atname,		"
						   +		"			attype,		"
						   +		"			tablename,	"
						   +		"			inherit,	"
						   +		"			required	"
						   +		"	FROM	dm.dm_fragmentattrs		"
						   +		"	WHERE	typeid=?	"
						   +		"	ORDER BY	atorder";
				   PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
				   stmt3.setInt(1,typeid);
				   ResultSet rs3 = stmt3.executeQuery();
				   while (rs3.next()) {
					   String atname = getString(rs3,1,"");
					   String attype = getString(rs3,2,"");
					   String tablename = getString(rs3,3,null);
					   String inherit = getString(rs3,4,"N");
					   if (inherit.equalsIgnoreCase("r")) {
						   // This is the result field
						   out.println("<result name=\""+atname+"\" />");
					   } else {
						   boolean required = getBoolean(rs3,5,false);
						   out.print("<parameter name=\""+atname+"\" type=\""+attype+"\" required=\""+(required?"Y":"N")+"\"");
						   if (tablename != null) {
							   out.println(">");
							   out.println("<tablename inherit=\""+inherit+"\">"+tablename+"</tablename>");
							   out.println("</parameter>");
						   } else {
							   out.println("/>");
						   }
					   }
				   }
				   rs3.close();
				   out.println("</fragment>");
			   }
			   rs2.close();
			   if (isFunction) {
				   out.println("</function>");
			   } else {
				   out.println("</action>");
			   }  
		   }
		   /*
		   else {
			   // action not found
		   }
		   */
		   rs.close();
	 } catch (SQLException e) {
		   e.printStackTrace();
	 } 
 }
 
 public void ExportGraphAsProcedure(int actionid,PrintWriter out)
 {
	 // Exports the specified graphical action as a procedure.
	 // To do this, we call the engine to convert the graph to DMScript.
	 // Then we wrap that with the appropriate XML to recreate it as a
	 // DMScript procedure (kind = 2)
	 //
	 Action action = getAction(actionid,true);
	 if (action != null) {
		 Domain d = action.getDomain();
		   if (d!=null) {
			   Engine engine = d.findNearestEngine();
			   CommandLine cmd = engine.showDMScript(action);
			   int res = cmd.run(true, null, true);
			   if (res == 0) {
				   // Engine converted action ok - create output
				   String DMScript = cmd.getOutput();
				   out.println("<action name=\""+action.getName()+"_converted\" summary=\""+action.getSummary()+"\" isGraphical=\"N\" category=\"\">");
				   out.println("<kind copy=\"N\">2</kind>");
				   out.println("<dmscript>");
				   byte[] encodedBytes = Base64.encodeBase64(DMScript.getBytes());
				   out.println(new String(encodedBytes));
				   out.println("</dmscript>");
				   // A Graphical flow can have no arguments (at least for now)
				   out.println("<arguments>");
				   out.println("</arguments>");
				   out.println("</action>");
			   }
		   }
	 }
 }
 
 public boolean CategoryInDomain(int id, int catid,int domainid,int t)
 {
	 // Returns "true" if a component (t=1), action (t=2) or procedure/function (t=3) exists in the specified
	 // category in the specified domain. Called by GetNewID when creating a new object so that it can construct
	 // the tree properly.
	 System.out.println("CategoryInDomain("+id+","+catid+","+domainid+","+t);
	 String sql="";
	 switch(t) {
	 case 1:
		 System.out.println("Checking how many categories are in domain "+domainid+" catid="+catid+" id="+id);
		 sql="SELECT count(*) FROM dm.dm_component a,dm.dm_component_categories b WHERE ? NOT IN (a.id,coalesce(a.parentid,a.id)) AND a.id = b.id AND b.categoryid=? AND a.domainid=?";
		 System.out.println(sql);
		 System.out.println(id+" "+catid+" "+domainid);
		 break;
	 case 2:
		 System.out.println("Checking how many categories are in domain "+domainid+" catid="+catid+" id="+id);
		 sql="SELECT count(*) FROM dm.dm_action a,dm.dm_action_categories b WHERE a.id != ? AND a.id = b.id AND b.categoryid=? AND a.domainid=? AND a.graphical='Y'";
		 System.out.println(sql);
		 System.out.println(id+" "+catid+" "+domainid);
		 break;
	 case 3:
		 System.out.println("Checking how many categories are in domain "+domainid+" catid="+catid+" id="+id);
		 sql = 	"SELECT	count(*)	"
		 +		"FROM	dm.dm_action a,	"
		 +		"		dm.dm_fragment_categories b,	"
		 +		"		dm.dm_fragments c	"
		 +		"WHERE 	a.id != ?	"
		 +		"AND	b.categoryid=?	"
		 +		"AND	a.domainid=?	"
		 +		"AND	a.graphical<>'Y'	"
		 +		"AND	a.id in (c.actionid,c.functionid)	"
		 +		"AND	b.id = c.id";
		 System.out.println(sql);
		 System.out.println(id+" "+catid+" "+domainid);
		 break;
	 default:
		 break;
	 }
	 
	 boolean res=false;
	 
	 try {
		 PreparedStatement stmt = getDBConnection().prepareStatement(sql);
		 stmt.setInt(1,id);
		 stmt.setInt(2,catid);
		 stmt.setInt(3,domainid);
		 ResultSet rs = stmt.executeQuery();
		 if (rs.next()) {
			 System.out.println("result is "+rs.getInt(1));
			 res=(rs.getInt(1)>0);
		 }
		 rs.close();
		 System.out.println("returning res="+res);
		 return res;
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	 return false;
 }
 
 public JSONObject GetActionReferences(int actionid)
 {
	 // Looks for actions, tasks, applications or components that reference the supplied
	 // action and returns JSON link for each one
	 JSONObject ret = new JSONObject();
	 String sql[]={
		"select 1,id,'Pre Action on Base Component' from dm.dm_component where preactionid="+actionid+" and parentid is null and status<>'D' "
		+ "union "
		+ "select 1,id,'Post Action on Base Component' from dm.dm_component where postactionid="+actionid+" and parentid is null and status<>'D' "
		+ "union "
		+ "select 1,id,'Custom Action on Base Component' from dm.dm_component where actionid="+actionid+" and parentid is null and status<>'D' "
		+ "union "
		+ "select 1,id,'Pre Action on Component Version' from dm.dm_component where preactionid="+actionid+" and parentid is not null and status<>'D' "
		+ "union "
		+ "select 1,id,'Post Action on Component Version' from dm.dm_component where postactionid="+actionid+" and parentid is not null and status<>'D' "
		+ "union "
		+ "select 1,id,'Custom Action on Component Version' from dm.dm_component where actionid="+actionid+" and parentid is not null and status<>'D' "
		+ "order by id",
		// Applications
		"select 2,id,'Pre Action on Base Application' from dm.dm_application where preactionid="+actionid+" and parentid is null and status<>'D' "
		+ "union "
		+ "select 2,id,'Post Action on Base Application' from dm.dm_application where postactionid="+actionid+" and parentid is null and status<>'D' "
		+ "union "
		+ "select 2,id,'Custom Action on Base Application' from dm.dm_application where actionid="+actionid+" and parentid is null and status<>'D' "
		+ "union "
		+ "select 2,id,'Pre Action on Application Version' from dm.dm_application where preactionid="+actionid+" and parentid is not null and status<>'D' "
		+ "union "
		+ "select 2,id,'Post Action on Application Version' from dm.dm_application where postactionid="+actionid+" and parentid is not null and status<>'D' "
		+ "union "
		+ "select 2,id,'Custom Action on Application Version' from dm.dm_application where actionid="+actionid+" and parentid is not null and status<>'D' "
		+ "order by id",
		// Stand-Alone Actions
		"select 3,a.domainid,'ActionTask \"'||a.name||'\" in Domain' from dm.dm_task a, dm.dm_taskaction b where a.id=b.id and b.actionid="+actionid,
		"select 3,domainid, 'PreLink to Task \"'||name||'\" in Domain' from dm.dm_task where preactionid="+actionid+" "
		+ "union "
		+ "select 3,domainid, 'PostLink to Task \"'||name||'\" in Domain' from dm.dm_task where postactionid="+actionid,
		"select 4,a.actionid, 'Used in Action' from dm.dm_actionfrags a,dm.dm_fragments b where a.typeid=b.id and "+actionid+" in (b.actionid,b.functionid)"
	 };
	 try {
		 Action action = getAction(actionid,false);
		 ret.add("name",action.getName());
		 JSONArray res = new JSONArray();
		 for (int i=0;i<sql.length;i++) {
			 PreparedStatement stmt = getDBConnection().prepareStatement(sql[i]);
			 ResultSet rs = stmt.executeQuery();
			 while (rs.next()) {
				 JSONObject obj = new JSONObject();
				 int type = rs.getInt(1);
				 int id = rs.getInt(2);
				 String desc = rs.getString(3);
				 switch(type) {
				 case 1: {
					 Component comp = getComponent(id,true);
					 if (comp != null) {
						 obj.add("desc",desc);
						 obj.add("data",comp.getLinkJSON());
						 res.add(obj);
					 }
				 	}
				 	break;
				 case 2: {
					 Application app = getApplication(id,true);
					 if (app != null) {
						 obj.add("desc",desc);
						 obj.add("data",app.getLinkJSON());
						 res.add(obj);
					 }
				 	}
				 	break;
				 case 3: {
					 Domain dom = getDomain(id);
					 if (dom != null) {
						 obj.add("desc",desc);
						 obj.add("data",dom.getLinkJSON());
						 res.add(obj);
					 }
				 	}
				 	break;
				 case 4: {
					 Action a = getAction(id,false);
					 if (a != null) {
						 obj.add("desc",desc);
						 obj.add("data",a.getLinkJSON());
						 res.add(obj);
					 }
				 	}
				 	break;
				 default:
					 break;
				 }
				 
			 }
			 rs.close();
		 }
		 ret.add("refs",res);
		 return ret;
	 } catch (SQLException e) {
		 System.out.println(e.getMessage());
		 e.printStackTrace();
		 return null;
	 }
 }
 
 public String getJSONFromServer(String url,Credential cred)
 {
	 // CloseableHttpClient httpclient = HttpClients.createDefault();
	 // Basic Auth setup
	 // System.out.println("getJSONFromServer: url="+url);

     byte[] credentials = null;
     if (cred != null) {
    	 try {
    		//System.out.println("Credential "+cred.getName()+" found");
	    	PreparedStatement stmt = getDBConnection().prepareStatement("select encusername,encpassword from dm.dm_credentials where id=?");
	     	stmt.setInt(1,cred.getId());
	     	ResultSet rs = stmt.executeQuery();
	     	if (rs.next()) {
	     		String un = rs.getString(1);
	     		String up = rs.getString(2);
	     		byte[] dun = Decrypt3DES(un,m_passphrase);
	     		byte[] dup = Decrypt3DES(up,m_passphrase);
	     		String credUsername = new String(dun);
	     		String credPassword = new String(dup);
	     		/*
	     		System.out.println(
	     		"credUsername=["+
	     		((credUsername!=null)?credUsername:"null")+
	     		"] credPassword=["+
	     		((credPassword!=null)?credPassword:"null")+
	     		"]");
	     		*/
	     		if (credUsername != null && credPassword != null) {
	     			// System.out.println("Setting credentials");
	     			credentials = Base64.encodeBase64((credUsername + ":" + credPassword).getBytes(StandardCharsets.UTF_8));
		     	}
	     	}
	     	rs.close();
    	 } catch(SQLException ex) {
    		 
    	 }
     } // else System.out.println("no credential");
     final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
     connManager.setMaxTotal(200);
     connManager.setDefaultMaxPerRoute(20);
     RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).build();
     CloseableHttpClient httpclient = HttpClients.custom()
     .setConnectionManager(connManager)
     .setDefaultRequestConfig(requestConfig)
     .build();
     
     CloseableHttpResponse response1=null;
     String resString="";
     
     try {
    	 HttpGet httpGet = new HttpGet(url);	 
    	 
		 if (credentials != null) {
			 httpGet.setHeader("Authorization", "Basic " + new String(credentials, StandardCharsets.UTF_8));
		 }
		 response1 = httpclient.execute(httpGet);
		 StatusLine sl = response1.getStatusLine();
		 int statusCode=0;
		 if (sl != null) {
			 statusCode = sl.getStatusCode();
		 } else {
			 System.out.println("*** could not get status line");
		 }
		 if (statusCode >= 200 && statusCode <=299) {
			 // In valid range
		     // System.out.println(response1.getStatusLine());
		     HttpEntity entity1 = response1.getEntity();
		     InputStream content = entity1.getContent();
		     // Consume all the result data, converting it to a string
		     byte d[] = new byte[2048];
		     int bytesRead = content.read(d);
		     while (bytesRead != -1) {
		    	 String block = new String(d,0,bytesRead);
		    	 resString+=block;
		    	 bytesRead = content.read(d);
		     }
		     while(content.read(d) != -1);
		     EntityUtils.consume(entity1);
		 } else {
			 System.out.println("Connecting to URL ["+url+"] returns Status Code "+statusCode);
		 }
	 } catch(HttpHostConnectException ex) {
		 // Failed to connect to the target server - it may be down.
		 System.out.println("Failed to connect to "+url+": "+ex.getMessage());
	 } catch(Exception ex) {
		 System.out.println("Exception of type "+ex.toString()+" thrown");
		 ex.printStackTrace();
	 } finally {
		 try {
			 if (response1 != null) response1.close();
			 httpclient.close();
			 if (response1 == null)
			 {
			  resString = "Could not connect to '" + url + "' using credentials '" + credentials + "'";
			 } 
		 } catch(IOException ex) {
			 // shrugs
		 }
	 }
	 return resString;
 }
 
 public String getDataSourceAttribute(int dsid,String attname)
 {
	 String sql = "select value,encrypted from dm.dm_datasourceprops where datasourceid=? and name=?";
	 String res = null;
	 try {
		 PreparedStatement st = getDBConnection().prepareStatement(sql);
		 st.setInt(1,dsid);
		 st.setString(2,attname);
		 ResultSet rs = st.executeQuery();
		 if (rs.next()) {
			 // found the attribute
			 res = rs.getString(1);
			 if (rs.getString(2).equalsIgnoreCase("y")) {
				 // Attribute is encrypted
				 res = new String(Decrypt3DES(res,m_passphrase));
			 }
		 }
		 rs.close();
		 st.close();
		 return res;
	 } catch (SQLException ex) {
		 return null;
	 }
 }
 

  public void internalLogin(ServletContext context)
  {
	  // Sets up internal login to run as "admin"
	  setUserID(1);
	  connectToDatabase(context);
	  GetDomains(1);
  }

  

  JSONObject getBuildHistoryJenkins(int builderid,int buildjobid)
  {
	  System.out.println("getBuildHistoryJenkins builderid="+builderid+" buildjobid="+buildjobid);
	  String sql2="select projectname from dm.dm_buildjob where id=?";
	  String sql3="select a.buildnumber,b.id,b.name,b.parentid from dm.dm_buildhistory a,dm.dm_component b where a.compid=b.id and	a.buildjobid=? order by buildnumber desc";
	  JSONObject ret = new JSONObject();
	  try {
	  	 Builder builder = getBuilder(builderid);
	  	 Credential cred = builder.getCredential();
		 PreparedStatement stmt2 = getDBConnection().prepareStatement(sql2);
		 PreparedStatement stmt3 = getDBConnection().prepareStatement(sql3);
		 stmt2.setInt(1,buildjobid);
		 stmt3.setInt(1,buildjobid);
		 String serverURL = getBuildServerURL(builderid);
		 if (serverURL != null) { 
			 System.out.println("Server URL="+serverURL);
			 ResultSet rs2 = stmt2.executeQuery();
			 if (rs2.next()) {
				 String jobname = rs2.getString(1); // .replaceAll(" ", "%20");
				 if (jobname.length()>0) {
					 //
					 // Get all the builds we know about for this build job and assemble the
					 // JSON Array of components associated with each build number
					 //
					 jobname = jobname.replaceAll(" ","%20");
					 Hashtable<Integer,JSONArray> complist = new Hashtable<Integer,JSONArray>();
					 ResultSet rs3 = stmt3.executeQuery();
					 while (rs3.next()) {
						 int buildno = rs3.getInt(1);
						 // Have we seen this build before?
						 JSONArray ja = complist.get(buildno);
						 if (ja == null) ja = new JSONArray();	// new build
						 int compid = rs3.getInt(2);
						 String compname = rs3.getString(3);
						 int parentid = getInteger(rs3,4,0);
						 JSONObject compdetails = new JSONObject();
						 compdetails.add("id",compid);
						 compdetails.add("name",compname);
						 compdetails.add("type",(parentid>0)?"cv":"co");
						 ja.add(compdetails);
						 complist.put(buildno,ja);						 
					 }
					 rs3.close();
					 //
					 // Now get "all" the builds from the build server. Not all of these will
					 // relate to builds in Release Engineer
					 //
					 boolean commit=false;
					 String res = getJSONFromServer(serverURL+"/job/"+jobname+"/api/json?tree=builds[number,timestamp,result,duration]",cred);
				     System.out.println(res);
					 JsonObject returnedjson = new JsonParser().parse(res).getAsJsonObject();
				     JsonArray builds = returnedjson.getAsJsonArray("builds");
				     JSONArray retBuilds = new JSONArray();
				     for (int i=0;i<builds.size();i++) {
				    	 JsonObject jsonJob = builds.get(i).getAsJsonObject();
				    	 int buildid = jsonJob.get("number").getAsInt();
				    	 int duration = jsonJob.get("duration").getAsInt();
				    	 String result = jsonJob.get("result").getAsString();
				    	 long timestamp = jsonJob.get("timestamp").getAsLong();
				    	 System.out.println("buildid="+buildid+" timestamp="+timestamp);
				    	 JSONObject buildobj = new JSONObject();
				    	 buildobj.add("id", buildid);
				    	 buildobj.add("result",result);
				    	 buildobj.add("timestamp",formatDateToUserLocale((int)(timestamp/1000)));
				    	 buildobj.add("duration",duration);
				    	 JSONArray recomps = complist.get(buildid);
				    	 if (recomps == null) recomps = new JSONArray();
				    	 buildobj.add("components",recomps);
				    	 retBuilds.add(buildobj);
				     }
				     ret.add("builds", retBuilds);
				     if (commit) getDBConnection().commit();
				 } else {
					 ret.add("error","Cannot retrieve Builds - Project Name is not set");
				 }
			 } else {
				 ret.add("error","Could not retrieve Project Name from build job");
			 }
			 rs2.close();
		 } else {
			 // Couldn't find server URL - stick an error into the return object
			 ret.add("error","Build Engine has no Server URL defined");
		 }
		 
		 stmt2.close();
	  } catch (SQLException e) {
			 System.out.println(e.getMessage());
			 e.printStackTrace();
			 ret.add("error","SQL Failed running getProjectsFromJenkins");
	  }
	  return ret;
}
  
  JSONObject getBuildHistoryBamboo(int builderid,int buildjobid)
  {
	  System.out.println("getBuildHistoryBamboo builderid="+builderid+" buildjobid="+buildjobid);
	  String sql1="select value,encrypted from dm.dm_buildengineprops where name='Server URL' and builderid = ?";
	  String sql2="select projectname from dm.dm_buildjob where id=?";
	  String sql3="select a.buildnumber,b.id,b.name,b.parentid from dm.dm_buildhistory a,dm.dm_component b where a.compid=b.id and	a.buildjobid=? order by buildnumber desc";
	  JSONObject ret = new JSONObject();
	  try {
	  	 Builder builder = getBuilder(builderid);
	  	 Credential cred = builder.getCredential();
		 PreparedStatement stmt1 = m_conn.prepareStatement(sql1);
		 PreparedStatement stmt2 = m_conn.prepareStatement(sql2);
		 PreparedStatement stmt3 = m_conn.prepareStatement(sql3);
		 stmt1.setInt(1,builderid);
		 stmt2.setInt(1,buildjobid);
		 stmt3.setInt(1,buildjobid);
		 ResultSet rs1 = stmt1.executeQuery();
		 if (rs1.next()) {
			 // Got the Server URL
			 String serverURL = rs1.getString(1);
			 String encrypted = rs1.getString(2);
			 System.out.println("Server URL="+serverURL);
			 if (encrypted != null && encrypted.equalsIgnoreCase("y")) {
				 serverURL = new String(Decrypt3DES(serverURL,m_passphrase));
				 System.out.println("Decrypted server URL="+serverURL);
			 }
			 ResultSet rs2 = stmt2.executeQuery();
			 if (rs2.next()) {
				 String jobname = rs2.getString(1).replaceAll(" ", "%20");
				 if (jobname.length()>0) {
					 //
					 // Get all the builds we know about for this build job and assemble the
					 // JSON Array of components associated with each build number
					 //
					 Hashtable<Integer,JSONArray> complist = new Hashtable<Integer,JSONArray>();
					 ResultSet rs3 = stmt3.executeQuery();
					 while (rs3.next()) {
						 int buildno = rs3.getInt(1);
						 // Have we seen this build before?
						 JSONArray ja = complist.get(buildno);
						 if (ja == null) ja = new JSONArray();	// new build
						 int compid = rs3.getInt(2);
						 String compname = rs3.getString(3);
						 int parentid = getInteger(rs3,4,0);
						 JSONObject compdetails = new JSONObject();
						 compdetails.add("id",compid);
						 compdetails.add("name",compname);
						 compdetails.add("type",(parentid>0)?"cv":"co");
						 ja.add(compdetails);
						 complist.put(buildno,ja);						 
					 }
					 rs3.close();
					 //
					 // Now get "all" the builds from the build server. Not all of these will
					 // relate to builds in DeployHub
					 //
					 boolean commit=false;
					 String res = getJSONFromServer(serverURL+"/rest/api/latest/job/"+jobname+"/api/json?tree=builds[number,timestamp,result,duration]",cred);
				     JsonObject returnedjson = new JsonParser().parse(res).getAsJsonObject();
				     JsonArray builds = returnedjson.getAsJsonArray("builds");
				     JSONArray retBuilds = new JSONArray();
				     for (int i=0;i<builds.size();i++) {
				    	 JsonObject jsonJob = builds.get(i).getAsJsonObject();
				    	 int buildid = jsonJob.get("number").getAsInt();
				    	 int duration = jsonJob.get("duration").getAsInt();
				    	 String result = jsonJob.get("result").getAsString();
				    	 long timestamp = jsonJob.get("timestamp").getAsLong();
				    	 System.out.println("buildid="+buildid+" timestamp="+timestamp);
				    	 JSONObject buildobj = new JSONObject();
				    	 buildobj.add("id", buildid);
				    	 buildobj.add("result",result);
				    	 buildobj.add("timestamp",formatDateToUserLocale((int)(timestamp/1000)));
				    	 buildobj.add("duration",duration);
				    	 JSONArray recomps = complist.get(buildid);
				    	 if (recomps == null) recomps = new JSONArray();
				    	 buildobj.add("components",recomps);
				    	 retBuilds.add(buildobj);
				     }
				     ret.add("builds", retBuilds);
				     if (commit) m_conn.commit();
				 } else {
					 ret.add("error","Cannot retrieve Builds - Project Name is not set");
				 }
			 } else {
				 ret.add("error","Could not retrieve Project Name from build job");
			 }
			 rs2.close();
		 } else {
			 // Couldn't find server URL - stick an error into the return object
			 ret.add("error","Build Engine has no Server URL defined");
		 }
		 rs1.close();
		 stmt1.close();
		 
		 stmt2.close();
	  } catch (SQLException e) {
			 System.out.println(e.getMessage());
			 e.printStackTrace();
			 ret.add("error","SQL Failed running getProjectsFromBamboo");
	  }
	  return ret;
  }
  
  JSONObject getProjectsFromJenkins(int builderid)
  {
	 System.out.println("getProjectsFromJenkins");
	 String serverURL = getBuildServerURL(builderid);
	 JSONObject ret = new JSONObject();
  	 Builder builder = getBuilder(builderid);
  	 Credential cred = builder.getCredential();

	 if (serverURL != null) {
		 // Got the Server URL
		 System.out.println("Server URL="+serverURL);
		 String res = getJSONFromServer(serverURL+"/api/json",cred);
		 if (res.startsWith("Could not connect")) {
			 ret.add("error",res);
		 } else {
			 JsonObject returnedjson = new JsonParser().parse(res).getAsJsonObject();
			 JsonArray jobs = returnedjson.getAsJsonArray("jobs");
			 JSONArray retJobs = new JSONArray();
			 if (jobs.size()==0) {
				 ret.add("error","No Projects found on Jenkins Server "+serverURL);
			 }
			 for (int i=0;i<jobs.size();i++) {
				 JsonObject jsonJob = jobs.get(i).getAsJsonObject();
				 String jobname = jsonJob.get("name").getAsString();
				 JSONObject jobobj = new JSONObject();
				 jobobj.add("name", jobname);
				 retJobs.add(jobobj);
			 }
			 ret.add("jobs", retJobs);
		 }  
	 } else {
		 // Couldn't find server URL - stick an error into the return object
		 ret.add("error","Build Engine has no Server URL defined");
	 }
	 return ret;
  }
  
  JSONObject getProjectsFromBamboo(int builderid)
  {
	  System.out.println("getProjectsFromBamboo");
	  String sql="select value,encrypted from dm.dm_buildengineprops where name='Server URL' and builderid = ?";
	  JSONObject ret = new JSONObject();
	  try {
	  	 Builder builder = getBuilder(builderid);
	  	 Credential cred = builder.getCredential();
		 PreparedStatement stmt = m_conn.prepareStatement(sql);
		 stmt.setInt(1,builderid);
		 ResultSet rs = stmt.executeQuery();
		 if (rs.next()) {
			 // Got the Server URL
			 String serverURL = rs.getString(1);
			 if (rs.getString(2).equalsIgnoreCase("y")) {
				 // Server URL is encrypted
				 serverURL = new String(Decrypt3DES(serverURL, m_passphrase));
			 }
			 System.out.println("Server URL="+serverURL);
			 String res = getJSONFromServer(serverURL+"/rest/api/latest/plan.json",cred);
			 if (res.startsWith("Could not connect")) {
				 ret.add("error",res);
			 } else {
			     JsonObject returnedjson = new JsonParser().parse(res).getAsJsonObject();
			     JsonObject plans = returnedjson.getAsJsonObject("plans");
			     JsonArray plan = plans.getAsJsonArray("plan");
			     JSONArray retJobs = new JSONArray();
			     if (plan.size()==0) {
			    	 ret.add("error","No Projects found on Bamboo Server "+serverURL);
			     }
			     for (int i=0;i<plan.size();i++) {
			    	 JsonObject jsonPlan = plan.get(i).getAsJsonObject();
			    	 String planname = jsonPlan.get("shortName").getAsString();
			    	 JSONObject jobobj = new JSONObject();
			    	 jobobj.add("name", planname);
			    	 retJobs.add(jobobj);
			     }
			     ret.add("jobs", retJobs);
			} 
		 } else {
			 // Couldn't find server URL - stick an error into the return object
			 ret.add("error","Build Engine has no Server URL defined");
		 }
		 rs.close();
		 stmt.close();
	  } catch (SQLException e) {
			 System.out.println(e.getMessage());
			 e.printStackTrace();
			 ret.add("error","SQL Failed running getProjectsFromBamboo");
	  }
	  return ret;
  }
  
  JSONObject getBuildHistory(int buildjobid)
  {
	 System.out.println("getBuildHistory buildjobid="+buildjobid);
	  
	 String sql=	"select	a.name,b.id	"
		  +		"from	dm.dm_providerdef	a,	"
		  +		"		dm.dm_buildengine	b,	"
		  +		"		dm.dm_buildjob		c	"
		  +		"where 	c.id=?	"
		  +		"and	c.builderid=b.id	"
		  +		"and	a.id=b.defid";
	 
	  try {
		  PreparedStatement stmt = getDBConnection().prepareStatement(sql);
		  stmt.setInt(1,buildjobid);
		  ResultSet rs = stmt.executeQuery();
		  if (rs.next()) {
			  // Got the provider name (this should always work)
			  String providerName = rs.getString(1);
			  int builderid = rs.getInt(2);
			  System.out.println("ProviderName="+providerName+" builderid="+builderid);
			  if (providerName.equalsIgnoreCase("jenkins")) {
				  rs.close();
				  stmt.close();
				  return getBuildHistoryJenkins(builderid,buildjobid);
			  } if (providerName.equalsIgnoreCase("bamboo")) {
				  rs.close();
				  stmt.close();
				  return getBuildHistoryBamboo(builderid,buildjobid);
			  } else {
				  // did not recognize provider
				  rs.close();
				  stmt.close();
				  JSONObject ret = new JSONObject();
				  ret.add("error","Provider Name \""+providerName+"\" not recognized");
				  return ret;
			  }
		  } else {
				 // Couldn't find provider - disaster
			  	rs.close();
			  	stmt.close();
			  	JSONObject ret = new JSONObject();
				ret.add("error","Could not find build engine provider name");
				return ret;
			 }
	  } catch (SQLException e) {
			 System.out.println(e.getMessage());
			 e.printStackTrace();
			 return null;
	  }
  }
  
  JSONObject getProjectsFromBuilder(int buildjobid,int buildengineid)
  {
	  System.out.println("getProjectsFromBuilder buildjobid="+buildjobid);
	  String sql;
	  if (buildjobid != -1) {
		  sql=	"select	a.name,b.id	"
			  +		"from	dm.dm_providerdef	a,	"
			  +		"		dm.dm_buildengine	b,	"
			  +		"		dm.dm_buildjob		c	"
			  +		"where 	c.id=?	"
			  +		"and	c.builderid=b.id	"
			  +		"and	a.id=b.defid";
	  } else {
		  sql = "select a.name,b.id	"
			  +		"from	dm.dm_providerdef	a,	"
			  +		"		dm.dm_buildengine	b	"
			  +		"where 	b.id=?	"
			  +		"and	a.id=b.defid";  
	  }
	  try {
		  PreparedStatement stmt = getDBConnection().prepareStatement(sql);
		  stmt.setInt(1,(buildjobid!=-1)?buildjobid:buildengineid);
		  ResultSet rs = stmt.executeQuery();
		  if (rs.next()) {
			  // Got the provider name (this should always work)
			  String providerName = rs.getString(1);
			  int builderid = rs.getInt(2);
			  System.out.println("ProviderName="+providerName+" builderid="+builderid);
			  if (providerName.equalsIgnoreCase("jenkins")) {
				  rs.close();
				  stmt.close();
				  return getProjectsFromJenkins(builderid);
			  } else if (providerName.equalsIgnoreCase("bamboo")) {
				  rs.close();
				  stmt.close();
				  return getProjectsFromBamboo(builderid);
			  } else {
				  // did not recognize provider
				  rs.close();
				  stmt.close();
				  JSONObject ret = new JSONObject();
				  ret.add("error","Provider Name \""+providerName+"\" not recognized");
				  return ret;
			  }
		  } else {
				 // Couldn't find provider - disaster
			  	rs.close();
			  	stmt.close();
			  	JSONObject ret = new JSONObject();
				ret.add("error","Could not find build engine provider name");
				return ret;
			 }
	  } catch (SQLException e) {
			 System.out.println(e.getMessage());
			 e.printStackTrace();
			 return null;
	  }
  }
  
  List<BuildJob> getBuildJobsForComponent(Component comp, int domid)
  {
	  System.out.println("getBuildJobsForComponent id="+comp.getId());
	  Domain d = null;
	  if (comp.getId() < 0) {
		  // New Component
		  d = getDomain(domid);
	  } else {
		  // Get the domain for this component
		  d = comp.getDomain();
	  }
	  String domainList="";
	  String sep="";
	  while (d != null) {
		  domainList+=sep+d.getId();
		  d = d.getDomain();
		  sep=",";
	  }
	  System.out.println("Domain List = "+domainList);
	  
	  String sql = "SELECT a.id,a.name,b.id	"
			  +	"FROM	dm.dm_buildjob		a,	"
			  + "		dm.dm_buildengine	b	"
			  + "WHERE	a.builderid=b.id		"
			  + "AND	b.domainid IN ("+domainList+")	"
			  + "AND	b.status='N'";
	  List <BuildJob> res = new ArrayList<BuildJob>();
	  try {
		  PreparedStatement stmt = getDBConnection().prepareStatement(sql);
		  ResultSet rs = stmt.executeQuery();
		  BuildJob ret = null;
		  while (rs.next()) {
			  ret = new BuildJob(this, rs.getInt(1), rs.getString(2));
			  ret.setBuilderId(rs.getInt(3));
			  res.add(ret);
		  }
		  rs.close();
		  stmt.close();
		  return res;
	  } catch(SQLException ex) {
		  System.out.println(ex.getMessage());
	  }
	  throw new RuntimeException("Unable to retrieve build jobs for component " + comp.getId());				
}
  
  // Moved SyncAnsible (and associated methods) into DMSession since it's better here.
  
  @SuppressWarnings("unused")
private String readUrl(String urlString) throws Exception
  {
    BufferedReader reader = null;
    try
    {
      URL url = new URL(urlString);
      try
      {
        reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
      }
      catch (FileNotFoundException e)
      {
        if (reader != null)
          reader.close();
        return "";
      }

      StringBuffer buffer = new StringBuffer();

      char[] chars = new char[1024];
      int read;
      while ((read = reader.read(chars)) != -1)
      {
        buffer.append(chars, 0, read);
      }
      String str = buffer.toString();
      return str;
    }
    finally
    {
      if (reader != null)
        reader.close(); 
    }
  }
  
  private String capitalize(String original)
  {
    if ((original == null) || (original.length() == 0))
    {
      return original;
    }
    String[] parts = original.split("_");
    if ((parts == null) || (parts.length == 1)) {
      return original.substring(0, 1).toUpperCase() + original.substring(1);
    }
    original = "";
    for (int i = 0; i < parts.length; i++)
    {
      if (parts[i].length() > 1)
        original = original + parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1);
      else
        original = original + parts[i].toUpperCase();
    }
    return original;
  }
  
  private static JsonObject convertToJson(String yamlString)
  {
    if ((yamlString == null) || (yamlString.trim().length() == 0)) {
      return new JsonObject();
    }
    Yaml yaml = new Yaml();
    Gson gson = new Gson();
    Object obj = yaml.load(yamlString);
    String jo = gson.toJson(obj);
    try
    {
      JsonObject jsonObject = new JsonParser().parse(jo).getAsJsonObject();
      return jsonObject;
    }
    catch (IllegalStateException e) {
    }
    return new JsonObject();
  }
  
  @SuppressWarnings("deprecation")
public void SyncAnsible(ServletContext context)
  {
	  internalLogin(context);
	  
	  int domainid = -1;
	  
	  try
	  {
	   PreparedStatement st;
	   st = getDBConnection().prepareStatement("select id from dm.dm_domain where name = 'Infrastructure'");

	   ResultSet rs2 = st.executeQuery();
	   while (rs2.next())
	   {
	    domainid = rs2.getInt(1);
	   }
	   rs2.close();
	   st.close();
	  }
   catch (SQLException e)
   {
   }
	  
	  if (domainid == -1)
	  {
    try
    {
     PreparedStatement st;
     st = getDBConnection().prepareStatement("insert into dm.dm_domain (id,name,domainid,ownerid,status) (select max(id)+1,'Infrastructure', 1, 1,'N' from dm.dm_domain)");
     st.execute();
     st.close();
     st = getDBConnection().prepareStatement("select id from dm.dm_domain where name = 'Infrastructure'");

     ResultSet rs2 = st.executeQuery();
     while (rs2.next())
     {
      domainid = rs2.getInt(1);
     }
     rs2.close();
     st.close();
    }
    catch (SQLException e)
    {
    }
	  }
	  
	  if (domainid == -1)
	   domainid = 1;
	  
	  int download_filter = 120;	// Number of downloads before we consider it worthy of import
	  String om_filter = System.getenv("OM_ANSIBLE_DOWNLOAD_FILTER");
	  if (om_filter != null) {
			try {
				download_filter = Integer.parseInt(om_filter);
			} catch (NumberFormatException e) {
				System.out.println("Invalid value "+om_filter+" for OM_ANSIBLE_DOWNLOAD_FILTER value");
			}
	  }
	  int numpages = 0;
	   
	  try {
	      String json = readUrl("https://galaxy.ansible.com/api/v1/roles/?format=json");
	      JsonParser jp = new JsonParser();
	      JsonElement je = jp.parse(json);
	      JsonObject ansible = je.getAsJsonObject();
	      numpages = ansible.get("num_pages").getAsInt();
	  } catch (Exception localException1) {
	  }

	  File tempdir = new File("temp/roles");
	  tempdir.mkdirs();
	  System.out.println("ROLES=" + tempdir.getAbsoluteFile());

	  // Base64 b64 = new Base64();

	  for (int page = 1; page < numpages; page++)
	  {
		  try
		  {
			  String json = readUrl("https://galaxy.ansible.com/api/v1/roles/?page=" + page + "&format=json");
			  JsonParser jp = new JsonParser();
			  JsonElement je = jp.parse(json);

			  JsonObject ansible = je.getAsJsonObject();
			  JsonArray results = (JsonArray)ansible.get("results");
			  for (int i = 0; i < results.size(); i++)
			  {
				  JsonObject role = results.get(i).getAsJsonObject();

				  String name = role.get("name").getAsString();
				  String namespace = role.get("namespace").getAsString();
				  String fullName = namespace + "." + name;

				  String actionName = name.replaceAll(" ", "_");
				  actionName = actionName.replaceAll("-", "_");

				  String user = role.get("github_user").getAsString();
				  String repo = role.get("github_repo").getAsString();
				  String summ = role.get("description").getAsString();
				  int dlcnt = role.get("download_count").getAsInt();

				  if (dlcnt >= download_filter)
				  { 
					  summ = StringEscapeUtils.escapeXml(summ);
					  JsonObject summfields = role.getAsJsonObject("summary_fields");
					  JsonArray tags = summfields.get("tags").getAsJsonArray();
					  String category = "General";
					  try
					  {
						  JsonObject tag = tags.get(0).getAsJsonObject();
						  category = tag.get("name").getAsString();
						  category = capitalize(category);
					  }
					  catch (IndexOutOfBoundsException localIndexOutOfBoundsException)
					  {
					  }

					  String default_vars_str = readUrl("https://raw.githubusercontent.com/" + user + "/" + repo + "/master/defaults/main.yml");
					  JsonObject default_vars_json = convertToJson(default_vars_str);

					  PrintWriter writer = new PrintWriter("temp/roles/" + user + "_" + repo + ".xml");
					  writer.println("<action name=\"ansible_" + actionName + "_" + user.replaceAll("-", "_") + "\" summary=\"" + summ + "\" isGraphical=\"N\" category=\"" + category + "\">");
					  writer.println("<kind copy=\"N\">3</kind>");
					  writer.println("<scripts>");
					  writer.println("<scriptbody filename=\"omansible.sh\">");
					  writer.println("</scriptbody>");
					  writer.println("</scripts>");
					  writer.println("<cmdline>");
					  writer.println("<script name=\"${DMHOME}/scripts/omansible.sh\" />");
					  writer.println("<flag name=\"Target\" switch=\"--ansible_target\" pad=\"false\" />");
					  writer.println("<flag name=\"TargetArg\" switch=\"${server.hostname}\" pad=\"false\" />");
					  writer.println("<flag name=\"GalaxyRole\" switch=\"--galaxy_role\" pad=\"false\" />");
					  writer.println("<flag name=\"GalaxyRoleArg\" switch=\"" + fullName + "\" pad=\"false\" />");
					  writer.println("<flag name=\"AnsibleSshUser\" switch=\"--ansible_ssh_user\" pad=\"false\" />");
					  writer.println("<flag name=\"AnsibleSshUserArg\" switch=\"'${AnsibleSshUser}'\" pad=\"false\" />");
					  writer.println("<flag name=\"AnsibleSshPassword\" switch=\"--ansible_ssh_pass\" pad=\"false\" />");
					  writer.println("<flag name=\"AnsibleSshPasswordArg\" switch=\"'${AnsibleSshPassword}'\" pad=\"false\" />");
					  writer.println("<flag name=\"AnsibleSudoPassword\" switch=\"--ansible_sudo_pass\" pad=\"false\" />");
					  writer.println("<flag name=\"AnsibleSudoPasswordArg\" switch=\"'${AnsibleSudoPassword}'\" pad=\"false\" />");

					  writer.println("<argument name=\"AnsibleVariableFile\" type=\"Entry\" inpos=\"0\" switchmode=\"S\" switch=\"--ansible_variable_file\" negswitch=\"\" pad=\"false\" required=\"false\" />");

					  if (default_vars_json.isJsonObject())
					  {
						  Set<Entry<String, JsonElement>> ens = default_vars_json.entrySet();
						  if (ens != null)
						  {
							  for (Iterator<Entry<String, JsonElement>> iterator = ens.iterator(); iterator.hasNext();)
							  {
								  Entry<String, JsonElement> en = iterator.next();
								  if (!((JsonElement)en.getValue()).isJsonPrimitive())
									  continue;
								  writer.println("<argument name=\"" + capitalize((String)en.getKey()) + "\" type=\"Entry\" inpos=\"0\" switchmode=\"S\" switch=\"--" + (String)en.getKey() + "\" negswitch=\"\" pad=\"false\" required=\"false\" />");
							  }
						  }

					  }

					  writer.println("</cmdline>");
					  writer.println("<fragment name=\"ansible_" + actionName + "_" + user.replaceAll("-", "_") + "_role\" summary=\"" + summ + "\">");
					  writer.println("<parameter name=\"AnsibleVariableFile\" type=\"Entry\" required=\"N\" default_value=\"${ansible_variable_file}\"/>");

					  HashMap<String,String> defaultvars = new HashMap<String, String>();
					  if (default_vars_json.isJsonObject())
					  {
						  Set<Entry<String, JsonElement>> ens = default_vars_json.entrySet();
						  if (ens != null)
						  {
							  for (Iterator<Entry<String, JsonElement>> iterator = ens.iterator(); iterator.hasNext();)
							  {
								  Entry<String, JsonElement> en = iterator.next();
								  if (!((JsonElement)en.getValue()).isJsonPrimitive())
									  continue;
								  defaultvars.put((String)en.getKey(), ((JsonElement)en.getValue()).toString().replaceAll("^\"|\"$", ""));
								  writer.println("<parameter name=\"" + capitalize((String)en.getKey()) + "\" type=\"Entry\" required=\"N\" default_value=\"${" + (String)en.getKey() + "}\" />");
							  }
						  }
					  }

					  writer.println("</fragment>");
					  writer.println(" </action>");
					  writer.close();
					  
					  int existingdomain = -1;
			    try
			    {
			     PreparedStatement st;
			     st = getDBConnection().prepareStatement("select domainid from dm.dm_action where name = ?");
        st.setString(1,"ansible_" + actionName + "_" + user.replaceAll("-", "_"));
        
			     ResultSet rs2 = st.executeQuery();
			     while (rs2.next())
			     {
			      existingdomain = rs2.getInt(1);
			     }
			     rs2.close();
			     st.close();
			    }
			    catch (SQLException e)
			    {
			    }
			    if (existingdomain == -1)
			     existingdomain = domainid;
			    
					  System.out.println("Import Domain=" + existingdomain);
					  ImportFunction(existingdomain, new File("temp/roles/" + user + "_" + repo + ".xml").getAbsolutePath());
					  long t = timeNow();

					  PreparedStatement st = getDBConnection().prepareStatement("select id from dm.dm_action where name = ?");
					  st.setString(1, "ansible_" + actionName + "_" + user.replaceAll("-", "_") + "_action");

					  int actionid = 0;

					  ResultSet rs2 = st.executeQuery();
					  while (rs2.next())
					  {
						  actionid = rs2.getInt(1);
					  }
					  rs2.close();
					  st.close();				  
					  
       existingdomain = -1;
       try
       {
        st = getDBConnection().prepareStatement("select domainid from dm.dm_action where name = ?");
        st.setString(1, "ansible_" + actionName + "_" + user.replaceAll("-", "_") + "_action");
        
        rs2 = st.executeQuery();
        while (rs2.next())
        {
         existingdomain = rs2.getInt(1);
        }
        rs2.close();
        st.close();
       }
       catch (SQLException e)
       {
       }
       if (existingdomain == -1)
        existingdomain = domainid;
       
					  int x;
					  if (actionid == 0)
					  {
						  st = getDBConnection().prepareStatement("INSERT INTO dm.dm_action (id,name,domainid,function,graphical,ownerid,creatorid,modifierid,created,modified,status,kind) VALUES(?,?,?,'N','Y',?,?,?,?,?,'N',6)");
						  actionid = getID("action");
						  st.setInt(1, actionid);
						  st.setString(2, "ansible_" + actionName + "_" + user.replaceAll("-", "_")  + "_action");
						  st.setInt(3, existingdomain);
						  st.setInt(4, 1);
						  st.setInt(5, 1);
						  st.setInt(6, 1);
						  st.setLong(7, t);
						  st.setLong(8, t);
						  st.execute();
						  st.close();

						  st = getDBConnection().prepareStatement("select id from dm.dm_category where name = ?");
						  st.setString(1, category);

						  int categoryid = 0;
						  rs2 = st.executeQuery();
						  while (rs2.next())
						  {
							  categoryid = rs2.getInt(1);
						  }
						  rs2.close();
						  st.close();

						  st = getDBConnection().prepareStatement("INSERT INTO dm.dm_action_categories (id,categoryid) VALUES(?,?)");
						  st.setInt(1, actionid);
						  st.setInt(2, categoryid);
						  st.execute();
						  st.close();

						  int procfuncid = 0;
						  int setcredid = 0;

						  st = getDBConnection().prepareStatement("select id from dm.dm_fragments where name = ?");
						  st.setString(1, "ansible_" + actionName + "_" + user.replaceAll("-", "_")  + "_role");

						  rs2 = st.executeQuery();
						  while (rs2.next())
						  {
							  procfuncid = rs2.getInt(1);
						  }
						  rs2.close();
						  //
						  // Check if ansible_set_credentials is set and add it before each
						  // call to the action if present.
						  //
						  st.setString(1,"ansible_set_credentials");
						  rs2 = st.executeQuery();
						  while (rs2.next())
						  {
							  setcredid = rs2.getInt(1);
							  if (rs2.wasNull()) setcredid=0;
						  }
						  rs2.close();
						  st.close();
						  int windowid=1;
						  if (setcredid>0)
						  {
							  // ansible_set_credentials is present
							  MoveNode(actionid, 1, 0, 400, 160, setcredid);
							  AddFlow(actionid, "0", "1", 1);
							  MoveNode(actionid, 2, 0, 400, 360, procfuncid);
							  AddFlow(actionid, "1", "2", 1);
							  windowid=2;
						  }
						  else
						  {
							  // ansible_set_credentials not present
							  MoveNode(actionid, 1, 0, 400, 160, procfuncid);
							  AddFlow(actionid, "0", "1", 1);
							  windowid=1;
						  }

						  List<FragmentAttributes> fields = getFragmentAttributes(actionid, windowid, 0);

						  Map<String,String> keyvals = new HashMap<String, String>();

						  //	            dm.UpdateFragAttrs(keyvals);
						  //
						  //	            fields = dm.getFragmentAttributes(actionid, windowid, 0);

						  keyvals = new HashMap<String, String>();
						  keyvals.put("a", new Integer(actionid).toString());
						  keyvals.put("w", Integer.toString(windowid));

						  if (fields != null)
						  {
							  for (x = 0; x < fields.size(); x++)
							  {
								  FragmentAttributes fa = (FragmentAttributes)fields.get(x);
								  String key = "f" + fa.getAttrId();
								  keyvals.put(key, fa.getDefaultValue());
							  }
						  }

						  UpdateFragAttrs(keyvals);
					  }

					  st = getDBConnection().prepareStatement("select count(*) from dm.dm_component where name = ?");
					  st.setString(1, "ansible_" + actionName + "_" + user.replaceAll("-", "_") );

					  int compcnt = 0;

					  rs2 = st.executeQuery();
					  while (rs2.next())
					  {
						  compcnt = rs2.getInt(1);
					  }
					  rs2.close();
					  st.close();

       existingdomain = -1;
       try
       {
        st = getDBConnection().prepareStatement("select domainid from dm.dm_action where name = ?");
        st.setString(1, "ansible_" + actionName + "_" + user.replaceAll("-", "_") + "_action");
        
        rs2 = st.executeQuery();
        while (rs2.next())
        {
         existingdomain = rs2.getInt(1);
        }
        rs2.close();
        st.close();
       }
       catch (SQLException e)
       {
       }
       if (existingdomain == -1)
        existingdomain = domainid;
       
					  if (compcnt != 0)
						  continue;
					  st = getDBConnection().prepareStatement("select id from dm.dm_category where name = ?");
					  st.setString(1, category);

					  int categoryid = 0;
					  rs2 = st.executeQuery();
					  while (rs2.next())
					  {
						  categoryid = rs2.getInt(1);
					  }
       rs2.close();
       st.close();
       
					  st = getDBConnection().prepareStatement("INSERT INTO dm.dm_component (id,name,domainid,ownerid,creatorid,modifierid,created,modified,status,filteritems,deployalways,actionid,comptypeid) VALUES(?,?,?,?,?,?,?,?,'N','Y','N',?,6)");
					  int compid = getID("component");
					  st.setInt(1, compid);
					  st.setString(2, "ansible_" + actionName + "_" + user.replaceAll("-", "_") );
					  st.setInt(3, existingdomain);
					  st.setInt(4, 1);
					  st.setInt(5, 1);
					  st.setInt(6, 1);
					  st.setLong(7, t);
					  st.setLong(8, t);
					  st.setLong(9, actionid);
					  st.execute();
					  st.close();
					  st = getDBConnection().prepareStatement("INSERT INTO dm.dm_component_categories (id,categoryid) VALUES(?,?)");
					  st.setInt(1, compid);
					  st.setInt(2, categoryid);
					  st.execute();
					  st.close();
					  getDBConnection().commit();

					  AttributeChangeSet changes = new AttributeChangeSet();
					  for (Iterator<Entry<String, String>> iterator = defaultvars.entrySet().iterator(); iterator.hasNext();)
					  {
						  Entry<String, String> entry = iterator.next();
						  String key = (String)entry.getKey();
						  String value = (String)entry.getValue();
						  changes.addAdded(new DMAttribute(key, value));
					  }

					  DMObject dmobj = getObject(ObjectType.COMPONENT, compid);
					  dmobj.updateAttributes(changes);
				  }
			  }
		  }
		  catch (Exception e)
		  {
			  e.printStackTrace();
		  }
	  }
  }

  public JSONObject getDeploymentDepsNodesEdges(int deployid)
  {
   JSONObject data = new JSONObject();   
   JSONArray nodes = new JSONArray();
   JSONArray edges = new JSONArray();

   ArrayList<String> added = new ArrayList<String>();
   ArrayList<String> edges_added = new ArrayList<String>();
   
   Deployment d = getDeployment(deployid,true);

   // add app
   if (!added.contains(d.getApplication().getOtid().toString()))
   {
    added.add(d.getApplication().getOtid().toString());
    nodes.add(new DeployDepsNode(d.getApplication().getOtid().toString(), d.getApplication().getName()).getJSON());
   } 

   // add env to app
   if (!added.contains(d.getEnvironment().getOtid().toString()))
   {
    added.add(d.getEnvironment().getOtid().toString());
    nodes.add(new DeployDepsNode(d.getEnvironment().getOtid().toString(), d.getEnvironment().getName()).getJSON());
   }
   edges_added.add(d.getApplication().getOtid().toString() + "-" + d.getEnvironment().getOtid().toString());
   edges.add(new DeployDepsEdge(d.getApplication().getOtid().toString(), d.getEnvironment().getOtid().toString()).getJSON());

   List<Server> servers = getServersInEnvironment(d.getEnvironment().getId());

   for (int i=0;i<servers.size();i++)
   {
    // add server to env
    Server srv = servers.get(i);
    if (!added.contains(srv.getOtid().toString()))
    {
     added.add(srv.getOtid().toString());
     nodes.add(new DeployDepsNode(srv.getOtid().toString(), srv.getName()).getJSON());
    } 
    edges_added.add(d.getEnvironment().getOtid().toString() + "-" + srv.getOtid().toString());
    edges.add(new DeployDepsEdge(d.getEnvironment().getOtid().toString(), srv.getOtid().toString()).getJSON());
   }

   boolean isRelease = false;

   if (d.getApplication().getIsRelease().compareToIgnoreCase("Y") == 0)
    isRelease = true;

   List<Component> comps = getComponents(ObjectType.APPLICATION, d.getApplication().getId(),isRelease);

   for (int i=0;i<comps.size();i++)
   {
    // add components to app
    Component comp = getComponent(comps.get(i).getId(),true);

    if (!added.contains(comp.getOtid().toString()))
    {
     added.add(comp.getOtid().toString());
     nodes.add(new DeployDepsNode(comp.getOtid().toString(), comp.getName()).getJSON());
    } 
    edges_added.add(comp.getOtid().toString() + "-" + d.getApplication().getOtid().toString());
    edges.add(new DeployDepsEdge(comp.getOtid().toString(), d.getApplication().getOtid().toString()).getJSON());

    List <Server> srvlist = GetServersWithComponentsDeployed(d.getEnvironment().getId(),comp.getId());

    for (int k=0;k<srvlist.size();k++)
    {
     // add components to server
     Server s = srvlist.get(k);
     edges_added.add(s.getOtid() + "-" + comp.getOtid());
     edges.add(new DeployDepsEdge(comp.getOtid().toString(), s.getOtid().toString()).getJSON());   
    }

    if (comp.getLastBuildNumber() > 0 && comp.getBuildJob() != null)
    {
     
     // add build number
     BuildJob bj = comp.getBuildJob();

     if (!added.contains(bj.getOtid().toString() + "_" + comp.getLastBuildNumber()))
     {
      added.add(bj.getOtid().toString() + "_" + comp.getLastBuildNumber());
      nodes.add(new DeployDepsNode(bj.getOtid().toString() + "_" + comp.getLastBuildNumber(), "Build #" + comp.getLastBuildNumber()).getJSON());
     }
     edges_added.add(bj.getOtid().toString() + "_" + comp.getLastBuildNumber() + "-" + comp.getOtid().toString());
     edges.add(new DeployDepsEdge(bj.getOtid().toString() + "_" + comp.getLastBuildNumber(), comp.getOtid().toString()).getJSON());

     if ( bj.getBuildCommitID(comp.getLastBuildNumber()) != null)
     {   
      ArrayList<Integer> builds = getPreviousBuildNumbers(comp.getId(), d.getEnvironment().getId(), d.getId()); 

      for (int j=0;j<builds.size();j++)
      {
       int bldnum = builds.get(j);

       if (bj.getBuildCommitID(bldnum) != null)
       {
        // add commits for build number (from last deployment's build to this one)
        if (!added.contains("cm" + bj.getBuildCommitID(bldnum)))
        {
         added.add("cm" + bj.getBuildCommitID(bldnum));
         nodes.add(new DeployDepsNode("cm" + bj.getBuildCommitID(bldnum), "Commit #\n" + bj.getBuildCommitID(bldnum)).getJSON());
        }
        edges_added.add("cm" + bj.getBuildCommitID(bldnum) + "-" + bj.getOtid().toString() + "_" + comp.getLastBuildNumber());
        edges.add(new DeployDepsEdge("cm" + bj.getBuildCommitID(bldnum), bj.getOtid().toString() + "_" + comp.getLastBuildNumber()).getJSON());

        ArrayList<String> files = getBuildFilesList(comp.getBuildJob().getId(), bldnum);

        for (int x=0;x<files.size();x++)
        {
         // add files to commit
         if (!added.contains("sr" + files.get(x)))
         {
          added.add("sr" + files.get(x));
          nodes.add(new DeployDepsNode("sr" + files.get(x), files.get(x)).getJSON());
         }
         edges_added.add("sr" + files.get(x) + "-" + "cm" + bj.getBuildCommitID(bldnum));
         edges.add(new DeployDepsEdge("sr" + files.get(x), "cm" + bj.getBuildCommitID(bldnum)).getJSON());
        }       
       }
      }
     } 
    }
   } 

   // Now handle other components on the servers
   for (int i=0;i<servers.size();i++)
   {
    comps = this.getComponentsOnServerList(servers.get(i));
    for (int k=0;k<comps.size();k++)
    {
     // add other components to servers
     Component comp = comps.get(k);
     if (!added.contains(comp.getOtid().toString()))
     {
      added.add(comp.getOtid().toString());
      nodes.add(new DeployDepsNode(comp.getOtid().toString(), comp.getName()).getJSON());
     } 
     
     if (!edges_added.contains(servers.get(i).getOtid() + "-" + comp.getOtid()))
      edges.add(new DeployDepsEdge(comp.getOtid().toString(), servers.get(i).getOtid().toString(),true).getJSON());
     
     comp = getComponent(comp.getId(),true);
     
     if (comp.getLastBuildNumber() > 0 && comp.getBuildJob() != null)
     {
      
      // add build number
      BuildJob bj = comp.getBuildJob();

      if (!added.contains(bj.getOtid().toString() + "_" + comp.getLastBuildNumber()))
      {
       added.add(bj.getOtid().toString() + "_" + comp.getLastBuildNumber());
       nodes.add(new DeployDepsNode(bj.getOtid().toString() + "_" + comp.getLastBuildNumber(), "Build #" + comp.getLastBuildNumber()).getJSON());
      }
      if (!edges_added.contains(bj.getOtid().toString() + "_" + comp.getLastBuildNumber() + "-" + comp.getOtid().toString()))
        edges.add(new DeployDepsEdge(bj.getOtid().toString() + "_" + comp.getLastBuildNumber(), comp.getOtid().toString(),true).getJSON());

      if ( bj.getBuildCommitID(comp.getLastBuildNumber()) != null)
      {   
       ArrayList<Integer> builds = getPreviousBuildNumbers(comp.getId(), d.getEnvironment().getId(), d.getId()); 

       for (int j=0;j<builds.size();j++)
       {
        int bldnum = builds.get(j);

        if (bj.getBuildCommitID(bldnum) != null)
        {
         // add commits for build number (from last deployment's build to this one)
         if (!added.contains("cm" + bj.getBuildCommitID(bldnum)))
         {
          added.add("cm" + bj.getBuildCommitID(bldnum));
          nodes.add(new DeployDepsNode("cm" + bj.getBuildCommitID(bldnum), "Commit #\n" + bj.getBuildCommitID(bldnum)).getJSON());
         }
         if (!edges_added.contains("cm" + bj.getBuildCommitID(bldnum) + "-" + bj.getOtid().toString() + "_" + comp.getLastBuildNumber()))
          edges.add(new DeployDepsEdge("cm" + bj.getBuildCommitID(bldnum), bj.getOtid().toString() + "_" + comp.getLastBuildNumber(),true).getJSON());

         ArrayList<String> files = getBuildFilesList(comp.getBuildJob().getId(), bldnum);

         for (int x=0;x<files.size();x++)
         {
          // add files to commit
          if (!added.contains("sr" + files.get(x)))
          {
           added.add("sr" + files.get(x));
           nodes.add(new DeployDepsNode("sr" + files.get(x), files.get(x)).getJSON());
          }
          if (!edges_added.contains("sr" + files.get(x) + "-" + "cm" + bj.getBuildCommitID(bldnum)))
             edges.add(new DeployDepsEdge("sr" + files.get(x), "cm" + bj.getBuildCommitID(bldnum),true).getJSON());
         }
        }
       }
      } 
     }
    } 
   }
   data.add("nodes",nodes);
   data.add("edges",edges);
   return data;
  }

  public ArrayList<Integer> getPreviousBuildNumbers(int compid,int envid, int logid)
  {
   String sql = "select distinct a.buildnumber from dm.dm_deploymentxfer a, dm.dm_serversinenv b where a.componentid = ? and a.serverid = b.serverid and b.envid = ? and deploymentid <= ? order by 1 desc";
   ArrayList<Integer> ret = new ArrayList<Integer>();
   try
   {
    PreparedStatement stmt = getDBConnection().prepareStatement(sql);
    stmt.setInt(1, compid);
    stmt.setInt(2, envid);
    stmt.setInt(3, logid);
    ResultSet rs = stmt.executeQuery();
    
    while(rs.next()) {
     ret.add(new Integer(rs.getInt(1)));
    }
    rs.close();
    stmt.close();
   }
   catch(SQLException ex)
   {
    ex.printStackTrace();
    rollback();
   } 
   return ret;
  }

  public Connection getDBConnection()
  {
   return m_conn;
  }

  public void setDBConnection(Connection m_conn)
  {
   this.m_conn = m_conn;
  }

  public Hashtable<Integer,int[]> getPollhash()
  {
   return m_pollhash;
  }

  public void setPollhash(Hashtable<Integer,int[]> m_pollhash)
  {
   this.m_pollhash = m_pollhash;
  }

  public int getUserID()
  {
   return m_userID;
  }

  public void setUserID(int m_userID)
  {
   this.m_userID = m_userID;
  }

  public String getCopyObjType()
  {
   return m_copyobjtype;
  }

  public void setCopyObjType(String m_copyobjtype)
  {
   this.m_copyobjtype = m_copyobjtype;
  }

  public int getCopyId()
  {
   return m_copyid;
  }

  public void setCopyId(int m_copyid)
  {
   this.m_copyid = m_copyid;
  }
  
  public String firstInstall()
  {
	  System.out.println("firstInstall");
	  String initialInstall="N";
	  LoginException res = connectToDatabase(m_httpSession.getServletContext());
	  if (res == null) {
		  // successful connection
		  try {
			  PreparedStatement stmt = m_conn.prepareStatement("SELECT lastlogin FROM dm.dm_user WHERE id=1");
			  ResultSet rs = stmt.executeQuery();
			  rs.next();
			  rs.getTimestamp(1);
			  if (rs.wasNull()) {
				  // admin user has not yet logged in.
				  initialInstall="Y";
			  }
			  rs.close();
			  stmt.close();
		  } catch(SQLException ex) {
			  System.out.println("firstInstall check encountered DB error:"+ex.getMessage());
			  initialInstall="Y";	// if DB error then it's likely we're still initialising database
		  }
	  } else {
		  System.out.println("Failed to connect to DB");
	  }
	  System.out.println("Returning initialInstall="+initialInstall);
	  return initialInstall;
  }
}
