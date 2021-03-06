/*
 * Created on May 2, 2006
 */
package org.openedit.entermedia.generators;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.ZipGroup;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.modules.MediaArchiveModule;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.generators.BaseGenerator;
import com.openedit.generators.Output;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;

public class DownloadZipGenerator extends BaseGenerator
{

	private static final Log log = LogFactory.getLog(DownloadZipGenerator.class);
	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager moduleManager)
	{
		fieldModuleManager = moduleManager;
	}

	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		MediaArchiveModule archiveModule = (MediaArchiveModule) getModuleManager().getBean("MediaArchiveModule");
		MediaArchive archive = archiveModule.getMediaArchive(inReq);
		
		String type = inReq.findValue("sourcefile");
		if( "attachments".equals( type ) )
		{
			ZipGroup zip = new ZipGroup();
			zip.setMediaArchive(archive);
			zip.setUser(inReq.getUser());
			Asset asset = archive.getAssetBySourcePath(inReq.getContentPage());
			zip.zipAttachments(asset, inOut.getStream() );
		}
		else
		{
			zipAssets(inReq, archive, archiveModule, inOut);
		}

	}

	protected void zipAssets(WebPageRequest inReq, MediaArchive archive, MediaArchiveModule archiveModulex, Output inOut)
	{
		Map<Asset, ConvertInstructions> assets = new HashMap<Asset, ConvertInstructions>();
		
		String catalogid = archive.getCatalogId();
		String[] assetids = inReq.getRequestParameters("assetselect_" + catalogid);
		if (assetids == null)
		{
			String hitssessionid = inReq.getRequestParameter("hitssessionid");
			if(hitssessionid == null)
			{
				return;
			}
			HitTracker hits = (HitTracker)inReq.getSessionValue(hitssessionid);
			for (Object object : hits) {
				String id = null;
				if( object instanceof Data)
				{
					id = ((Data)object).getId();
				}
				
				if(id == null)
				{
					continue;
				}
				
				Asset asset = archive.getAsset(id);
				ConvertInstructions ins = new ConvertInstructions();
				ins.setAssetSourcePath(asset.getSourcePath());
				assets.put(asset, ins);
			}
		
		}
		else
		{
			//Todo use originals page request?
			for (String assetid : assetids)
			{
				Asset asset = archive.getAsset(assetid);
				if(asset == null)
				{
					log.warn("Cannot add asset with id '" + assetid + "': does not exist in catalog " + catalogid);
					continue;
				}
				ConvertInstructions ins = new ConvertInstructions();
				inReq.putPageValue("asset",asset);
				//archive.loadAssetPermissions(asset.getSourcePath(), inReq);
				
				ins.addPageProperties(inReq.getPage());
				ins.addPageValues(inReq.getPageMap());
				ins.setAssetSourcePath(asset.getSourcePath());
				//ins.setSourceFile("original");
				
				String height = inReq.getRequestParameter(catalogid + "." + assetid + ".height");
				String width = inReq.getRequestParameter(catalogid + "." + assetid + ".width");
				
				if( width !=null )
				{
					int w = Integer.parseInt(width);
					int h = -1;
					if (height == null)
					{
						h = Integer.MAX_VALUE;
					}
					else
					{
						h = Integer.parseInt(height);
					}
					ins.setMaxScaledSize(w, h);
				}
				String extension = inReq.getRequestParameter(catalogid + "." + assetid + ".extension");
				if (extension != null)
				{
					ins.setOutputExtension(extension);
				}
				/*
				Boolean watermark = (Boolean)inReq.getPageValue("canforcewatermark");
				if (watermark)
				{
					ins.setWatermark(Boolean.valueOf(watermark));
					ins.setOutputExtension("jpg");
				}
				String orginal = ins.getProperty("candownload");
				if (!Boolean.parseBoolean(orginal))
				{
					if( ins.getMaxScaledSize() == null )
					{
						ins.setOutputExtension("jpg");
						ins.setMaxScaledSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
					}
				}
//				String watermark = inReq.getRequestParameter(catalogid + "." + assetid + ".watermark");
//				if(Boolean.parseBoolean(watermark)){
//					ins.setWatermark(true);
//				}
				*/
				assets.put(asset, ins);
			}
		}
		ZipGroup zip = new ZipGroup();
		zip.setMediaArchive(archive);
		zip.setUser(inReq.getUser());
		zip.zipItems(assets, inOut.getStream());
	}

	public boolean canGenerate(WebPageRequest inReq)
	{
	
		boolean ok =  inReq.getPage().getMimeType().equals("application/x-zip");
		return ok;
	}

}
