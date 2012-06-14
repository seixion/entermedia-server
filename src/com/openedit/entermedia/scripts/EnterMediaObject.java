package com.openedit.entermedia.scripts;

import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;

import java.io.File;

import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.MediaArchive;
import org.openedit.events.PathEventManager;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.servlet.OpenEditEngine;
import com.openedit.users.UserManager;

public class EnterMediaObject
{
	protected WebPageRequest context;
	protected ModuleManager moduleManager;
	protected ScriptLogger log;
	protected GroovyScriptEngine engine;
	protected UserManager userManager;
	//protected PageManager pageManager;
	protected File root;
	public MediaArchive getMediaArchive()
	{
		return (MediaArchive)context.getPageValue("mediaarchive");
	}

	public WebPageRequest getContext()
	{
		return context;
	}
	public void setContext(WebPageRequest inContext)
	{
		context = inContext;
	}
	public ModuleManager getModuleManager()
	{
		return moduleManager;
	}
	public void setModuleManager(ModuleManager inModuleManager)
	{
		moduleManager = inModuleManager;
	}
	public ScriptLogger getLog()
	{
		return log;
	}
	public void setLog(ScriptLogger inLog)
	{
		log = inLog;
	}
	public GroovyScriptEngine getEngine()
	{
		return engine;
	}
	public void setEngine(GroovyScriptEngine inEngine)
	{
		engine = inEngine;
	}
	public UserManager getUserManager()
	{
		return userManager;
	}
	public void setUserManager(UserManager inUserManager)
	{
		userManager = inUserManager;
	}
	public PageManager getPageManager()
	{
		return (PageManager)getModuleManager().getBean("pageManager");
	}
	public void setPageManager(PageManager inPageManager)
	{
		//pageManager = inPageManager;
	}
	public File getRoot()
	{
		return root;
	}
	public void setRoot(File inRoot)
	{
		root = inRoot;
	}

	private Object getBean(String inName)
	{
		try {
			GroovyClassLoader loader = getEngine().getGroovyClassLoader();
			Class groovyClass = loader.loadClass(inName);
			
			Object object = groovyClass.newInstance();
			return object;
		} catch (Exception e) {
			throw new OpenEditException(e);
		} 
	}

	public boolean assertNotNull(Object inObj, String inMessage)
	{
		if( inObj == null)
		{
			log.info(inMessage + " was null");
			return false;
		}
		return true;
	}
	public boolean assertEquals(Object inWhat, Object inEquals, String inMessage)
	{
		boolean ok = assertEquals(inWhat, inEquals);
		if( !ok )
		{
			log.info(inMessage);
		}
		return ok;
	}
	public boolean assertEquals(Object inWhat, Object inEquals)
	{
		if( inWhat == null || !inWhat.equals(inEquals))
		{
			log.error(inWhat + " != " + inEquals);
			return false;
		}
		return true;
	}
	public boolean assertTrue(Object inCheck)
	{
		if(!Boolean.parseBoolean(String.valueOf( inCheck ) ) )
		{
			log.error("Not true: ${inCheck}");
			return false;
		}
		return true;
	}
	
	public WebPageRequest createPageRequest(String inPath)
	{
		Page page = getPageManager().getPage(inPath);
		return context.copy(page);
	}

	public OpenEditEngine getOpenEditEngine()
	{
		return (OpenEditEngine)getModuleManager().getBean("OpenEditEngine");
	}

	public SearcherManager getSearcherManager()
	{
		return (SearcherManager)getModuleManager().getBean("searcherManager");
	}

	public PathEventManager getPathEventManager()
	{
		String catalogid = context.findValue("catalogid");
		return (PathEventManager)getModuleManager().getBean(catalogid,"pathEventManager");
	}

	public Searcher loadSearcher(WebPageRequest inReq) throws Exception
	{
		String fieldname = inReq.findValue("searchtype");
		if (fieldname == null)
		{
			return null;
		}
		String catalogId = inReq.findValue("catalogid");

		org.openedit.data.Searcher searcher = getSearcherManager().getSearcher(catalogId, fieldname);
		return searcher;
	}

	
}
