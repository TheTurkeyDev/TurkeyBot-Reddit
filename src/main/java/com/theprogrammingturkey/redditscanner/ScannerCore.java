package com.theprogrammingturkey.redditscanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theprogrammingturkey.ggserver.ServerCore;
import com.theprogrammingturkey.ggserver.ServerCore.Level;
import com.theprogrammingturkey.ggserver.services.IServiceCore;
import com.theprogrammingturkey.ggserver.services.ServiceManager;

public class ScannerCore implements IServiceCore
{
	public static final String SERVICE_ID = "redditbot";
	private static final JsonParser JSON = new JsonParser();

	private ScannerThread commentScanner;

	@Override
	public String getServiceID()
	{
		return SERVICE_ID;
	}

	@Override
	public String getServiceName()
	{
		return "TurkeyBot - Reddit";
	}

	@Override
	public void init()
	{
		commentScanner = new ScannerThread();
		
		try
		{
			File config = new File(ServiceManager.getConfigFolder().getPath() + "/turkeybot-reddit-config.json");
			if(!config.exists())
			{
				config.getParentFile().mkdirs();
				config.createNewFile();
			}
			else
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(config)));
				StringBuilder result = new StringBuilder();
				String line = "";
				while((line = reader.readLine()) != null)
					result.append(line);
				reader.close();
				JsonObject json = JSON.parse(result.toString()).getAsJsonObject();
				for(JsonElement keyword: json.getAsJsonArray("keywords"))
					commentScanner.keywords.add(keyword.getAsString());
				for(JsonElement subreddit: json.getAsJsonArray("subreddits"))
					commentScanner.subReddits.add(subreddit.getAsString());
			}
		} catch(Exception e)
		{
			ServerCore.output(Level.Error, "Reddit Bot", "Failed to load config!");
		}

		commentScanner.init();
	}

	@Override
	public void stop()
	{
		commentScanner.endThread();
	}
}
