package org.openedit.entermedia.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openedit.Data;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.profile.UserProfile;

import com.openedit.OpenEditException;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.util.Replacer;

public class AssetSecurityDataArchive implements AssetSecurityArchive
{

	protected SearcherManager fieldSearcherManager;
	protected Replacer fieldReplacer;

	@Override
	public List getAccessList(MediaArchive inArchive, Asset inAsset) throws OpenEditException
	{
		return getAccessList(inArchive,"view",inAsset);
	}
	public List getAccessList(MediaArchive inArchive, String inType, Asset inAsset) throws OpenEditException
	{

		if (inAsset.isPropertyTrue("public"))
		{
			List permission = new ArrayList();
			permission.add("true");
			return permission; //Nothing else matters
		}

		Set<String> permissions = new HashSet(loadBasePermissions(inArchive,inType));

		String users = inAsset.get( inType + "users");
		if (users != null)
		{
			permissions.addAll(asList("user_", users.split("\\s+")));
		}

		String groups = inAsset.get(inType + "groups");
		if (groups != null)
		{
			permissions.addAll(asList("group_", groups.split("\\s+")));
		}

		// What Libraries is this asset part of?
		String libraries = inAsset.get(inType + "libraries");
		if (libraries != null)
		{
			permissions.addAll(asList("library_", libraries.split("\\s+")));
		}

		// clean up variables? add a bunch, then they can resolve in index time
		// tmp.put("asset.owner", inAsset.get("owner"));
		Map tmp = new HashMap();

		List values = new ArrayList(permissions.size());
		tmp.put("asset.owner", inAsset.get("owner"));
		for (Iterator iterator = permissions.iterator(); iterator.hasNext();)
		{
			String value = (String) iterator.next();
			value = getReplacer().replace(value, tmp);
			values.add(value);
		}
		return values;
	}

	protected Collection asList(String inPrefix, String[] inSplit)
	{
		for (int i = 0; i < inSplit.length; i++)
		{
			inSplit[i] = inPrefix + inSplit[i];
		}
		List things = Arrays.asList(inSplit);
		return things;
	}

	protected List<String> loadBasePermissions(MediaArchive inArchive, String inType)
	{
		List<String> permissions = new ArrayList();

		collectUsers(inArchive, "catalogasset" + inType + "users", "user_", permissions);
		collectUsers(inArchive, "catalogasset" + inType + "groups", "group_", permissions);

		// collectUsers(inArchive, "catalogassetviewlibraries" , permissions);

		return permissions;
	}

	protected void collectUsers(MediaArchive inArchive, String inType, String inPrefix, List permissions)
	{
		Data value = getSearcherManager().getData(inArchive.getCatalogId(), "catalogsettings", inType);
		if (value != null)
		{
			String groups = value.get("value");
			if (groups != null)
			{
				permissions.addAll(asList(inPrefix, groups.split("\\s+")));
			}
		}
	}

	@Override
	public void revokeViewAccess(MediaArchive inArchive, String inUsername, Asset inAsset)
	{
		Collection users = inAsset.getValues("viewusers");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}
		users.remove(inUsername);
		inAsset.setValues("viewusers", users);
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void revokeGroupViewAccess(MediaArchive inArchive, String inGroupname, Asset inAsset)
	{
		Collection<String> users = inAsset.getValues("viewgroups");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}
		users.remove(inGroupname);
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantViewAccess(MediaArchive inArchive, String inUsername, Asset inAsset) throws OpenEditException
	{

		Collection<String> users = inAsset.getValues("viewusers");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}

		users.add(inUsername);
		inAsset.removeProperty("public");
		inAsset.setValues("viewusers", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantGroupViewAccess(MediaArchive inArchive, String inGroupname, Asset inAsset) throws OpenEditException
	{
		Collection<String> users = inAsset.getValues("viewgroups");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}
		users.add(inGroupname);
		inAsset.removeProperty("public");
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void grantGroupViewAccess(MediaArchive inArchive, Collection<String> inGroupnames, Asset inAsset) throws OpenEditException
	{
		Collection<String> users = inAsset.getValues("viewgroups");
		if (users == null)
		{
			users = new ArrayList<String>();
		}
		else
		{
			users = new ArrayList<String>(users);
		}
		users.addAll(inGroupnames);
		inAsset.removeProperty("public");
		inAsset.setValues("viewgroups", users);
		inArchive.saveAsset(inAsset, null);

	}

	@Override
	public void grantAllAccess(MediaArchive inArchive, Asset inAsset)
	{
		inAsset.removeProperty("viewgroups");
		inAsset.removeProperty("viewusers");
		inAsset.setProperty("public", "true");
		inArchive.saveAsset(inAsset, null);
	}

	@Override
	public void clearAssetPermissions(MediaArchive inArchive, Asset inAsset)
	{
		// TODO Auto-generated method stub
		inAsset.removeProperty("public");
		inAsset.removeProperty("viewgroups");
		inAsset.removeProperty("viewusers");
		inArchive.saveAsset(inAsset, null);

	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Replacer getReplacer()
	{
		if (fieldReplacer == null)
		{
			fieldReplacer = new Replacer();
		}
		return fieldReplacer;
	}

	public Boolean canDo(MediaArchive inArchive, User inUser, UserProfile inProfile, String inType, Asset inAsset)
	{
		Collection allowed = getAccessList(inArchive,inType, inAsset);
		if( allowed.size() == 0 )
		{
			return false;
		}
		for (Iterator iterator = inUser.getGroups().iterator(); iterator.hasNext();)
		{
			Group group = (Group) iterator.next();
			if( allowed.contains( "group_" + group.getId() ) )
			{
				return Boolean.TRUE;
			}
		}
		//TODO: Add libraries from user , profile and each group
		
		if( allowed.contains("user_" + inUser.getUserName()))
		{
			return Boolean.TRUE;
		}
		
		return false;
	}

}