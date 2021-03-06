package publishing;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*
import org.openedit.event.*

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.Page

import org.entermedia.locks.Lock;


public void init() {

	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos

	Searcher queuesearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "publishqueue");


	SearchQuery query = queuesearcher.createSearchQuery();
	WebEvent webevent = context.getPageValue("webevent");
	Asset asset = null;
	if( webevent != null)
	{
		String sourcepath = webevent.getSourcePath();
		if( sourcepath != null )
		{
			asset = mediaArchive.getAssetBySourcePath(sourcepath);
			if( asset != null)
			{
				query.addExact("assetid",asset.getId());
			}
		}
	}
	String assetid = context.getRequestParameter("assetid");
	if(assetid != null){
		query.addExact("assetid", assetid);
	}
	query.addOrsGroup("status","new pending retry");
	query.addNot("remotepublish","true");
	
	HitTracker tracker = queuesearcher.search(query);
	log.info("publishing " + tracker.size() + " assets" + queuesearcher);
	if( tracker.size() > 0)
	{
		for( Data result:tracker)
		{
			Data publishrequest = queuesearcher.searchById(result.getId());
			if( publishrequest == null)
			{
				log.error("Publish queue index out of date");
				continue;
			}
			
			assetid = result.get("assetid");
			asset = mediaArchive.getAsset(assetid);
//			}
//			String resultassetid = result.get("assetid");
//			if(asset != null && !asset.getId().equals(resultassetid))
//			{
//				asset = mediaArchive.getAsset(resultassetid );
//			}

			String presetid = publishrequest.get("presetid");
			Data preset = mediaArchive.getSearcherManager().getData(mediaArchive.getCatalogId(), "convertpreset", presetid);
					
			String publishdestination = publishrequest.get("publishdestination");
			Data destination = mediaArchive.getSearcherManager().getData(mediaArchive.getCatalogId(), "publishdestination",publishdestination);

			try
			{
				Publisher publisher = getPublisher(mediaArchive, destination.get("publishtype"));
				Lock lock = mediaArchive.getLockManager().lockIfPossible(mediaArchive.getCatalogId(), "assetpublish/" + asset.getSourcePath(), "admin");
				if( lock == null)
				{
					log.info("asset already being published ${asset}");
					continue;
				}
				PublishResult presult = null;
				try
				{
					presult = publisher.publish(mediaArchive,asset,publishrequest, destination,preset);
				}
				finally
				{
					mediaArchive.releaseLock(lock);
				}
				if (presult == null)
				{
					log.info("result from publisher is null, continuing");
					continue;
				}
				if( presult.isError() )
				{
					publishrequest.setProperty('status', 'error');
					publishrequest.setProperty("errordetails", presult.getErrorMessage());
					queuesearcher.saveData(publishrequest, context.getUser());
					firePublishEvent(publishrequest.getId());
					continue;
				}
				if( presult.isComplete() )
				{
					log.info("Published " +  asset + " to " + destination);
					publishrequest.setProperty('status', 'complete');
					publishrequest.setProperty("errordetails", " ");
					queuesearcher.saveData(publishrequest, context.getUser());
					firePublishEvent(publishrequest.getId());
				}
				else if( presult.isPending() )
				{
					publishrequest.setProperty('status', 'pending');
					publishrequest.setProperty("errordetails", " ");
					queuesearcher.saveData(publishrequest, context.getUser());
				}
				//check for remotempublishstatus?
			}
			catch( Throwable ex)
			{
				log.error("Problem publishing ${asset} to ${publishdestination}", ex);
				publishrequest.setProperty('status', 'error');
				if(ex.getCause() != null)
				{
					ex = ex.getCause();
				}
				publishrequest.setProperty("errordetails", "${destination} publish failed ${ex}");
				queuesearcher.saveData(publishrequest, context.getUser());
			}
			asset = null; //This is kind of crappy code.
		
		}
	}
}


protected firePublishEvent(String inOrderItemId)
{
	WebEvent event = new WebEvent();
	event.setSearchType("publishqueue");
	event.setProperty("publishqueueid", inOrderItemId);
	event.setOperation("publishing/publishcomplete");
	event.setUser(context.getUser());
	event.setCatalogId(mediaarchive.getCatalogId());
	mediaarchive.getMediaEventHandler().eventFired(event);

}

protected Publisher getPublisher(MediaArchive inArchive, String inType)
{
//	GroovyClassLoader loader = engine.getGroovyClassLoader();
//	Class groovyClass = loader.loadClass("publishing.publishers.${inType}publisher");
//	Publisher publisher = (Publisher) groovyClass.newInstance();
//	return publisher;
	return moduleManager.getBean("${inType}publisher");
}

init();