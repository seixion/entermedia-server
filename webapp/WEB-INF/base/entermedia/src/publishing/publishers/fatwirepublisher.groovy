package publishing.publishers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*
import java.net.URL;
import com.openedit.page.Page
import com.openedit.util.FileUtils

import com.openedit.util.RequestUtils
import com.openedit.users.UserManager
import com.openedit.users.User



public class fatwirepublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(fatwirepublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		
		//setup result object
		PublishResult result = new PublishResult();
		
		String exportname = inPublishRequest.get("exportname");
		String urlHome = inPublishRequest.get("homeurl");
		String username =  inPublishRequest.get("username");
		UserManager usermanager = (UserManager) mediaArchive.getModuleManager().getBean("userManager");
		User inUser = usermanager.getUser(username);
		String copyrightstatus = inAsset.get("copyrightstatus");
		Searcher searcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "copyrightstatus");
		String usage = null;
		if (copyrightstatus!=null)
		{
			Data data = searcher.searchById(copyrightstatus);
			usage = data.get("name");
		}
		String fatwireId = inAsset.get("fatwireid");
		if (fatwireId != null)
		{
			result.setComplete(true);
			result.setErrorMessage("Not exporting asset, already exists on Fatwire");
			return result;
		}
		else
		{
			//this does the actual publishing
			Object fatwireManager = mediaArchive.getModuleManager().getBean( "fatwireManager");
			try {
				fatwireManager.setMediaArchive(mediaArchive);
				Object assetBean = fatwireManager.pushAsset(inAsset, inUser, urlHome, usage, exportname);//testPut();//
				if (assetBean != null)
				{
					String newId = assetBean.getId();
					String status = assetBean.getStatus();
					log.info("response from publishing request to FatWire: newId ${newId} status ${status}");
					inPublishRequest.setProperty("trackingnumber",newId);
					result.setComplete(true);
				}
				else 
				{
					log.info("Error publishing asset: asset bean is NUll");
					result.setComplete(true);
					result.setErrorMessage("Error publishing to FatWire: unable to publish asset");
				}
			}
			catch (IOException e)
			{
				log.error(e.getMessage(), e);
				result.setComplete(true);
				result.setErrorMessage(e.getMessage());
			}
			catch (Exception e)
			{
				log.error(e.getMessage(), e);
				result.setComplete(true);
				result.setErrorMessage(e.getMessage());
			}
		}
		return result;
	}
}