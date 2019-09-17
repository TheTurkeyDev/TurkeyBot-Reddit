package com.theprogrammingturkey.redditscanner;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theprogrammingturkey.ggserver.ServerCore;
import com.theprogrammingturkey.ggserver.ServerCore.Level;
import com.theprogrammingturkey.ggserver.news.INewsData;
import com.theprogrammingturkey.ggserver.news.NewsDispatcher;
import com.theprogrammingturkey.volatiliaweb.WebRequestBuilder;

public class ScannerThread implements Runnable
{
	private static final JsonParser PARSER = new JsonParser();

	private boolean run = false;
	private Thread thread;

	private String lastComment = "";

	public List<String> keywords = new ArrayList<>();
	public List<String> subReddits = new ArrayList<>();

	private int failedAttemptsRow = 0;

	@Override
	public void run()
	{
		ServerCore.output(Level.Info, "Reddit Bot", "Scanning reddit posts and comments");
		while(run)
		{
			runCommentScan();
			try
			{
				Thread.sleep(10000);
			} catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void runCommentScan()
	{
		for(String subReddit : this.subReddits)
			try
			{
				WebRequestBuilder request = new WebRequestBuilder("https://www.reddit.com/r/" + subReddit + "/comments/.json");
				request.addURLProp("limit", "100");
				if(!lastComment.equalsIgnoreCase(""))
					request.addURLProp("before", lastComment);
				request.addHeaderProp("User-Agent", "User-Agent: com.theprogrammingturkey.redditscanner:v0.5 (by /u/turkey2349)");
				String response = request.executeRequest();
				JsonObject json = PARSER.parse(response).getAsJsonObject();
				JsonArray comments = json.get("data").getAsJsonObject().get("children").getAsJsonArray();
				for(int i = 0; i < comments.size(); i++)
				{
					JsonObject commentData = comments.get(i).getAsJsonObject().get("data").getAsJsonObject();
					if(i == 0)
					{
						String id = commentData.get("id").getAsString();
						lastComment = "t1_" + id;
					}

					String thread = commentData.get("link_title").getAsString();
					String body = commentData.get("body").getAsString();

					for(String keyword : this.keywords)
					{
						if(body.toLowerCase().contains(keyword.toLowerCase()) || thread.toLowerCase().contains(keyword.toLowerCase()))
						{
							String title = "\"" + keyword + "\" was found in thread: " + thread;
							CommentData newsData = new CommentData(title, body);
							NewsDispatcher.dispatch(newsData);
						}
					}
				}
				this.failedAttemptsRow = 0;
			} catch(Exception e)
			{
				failedAttemptsRow++;
				if(failedAttemptsRow % (5 * this.subReddits.size()) == 0)
					ServerCore.output(Level.Info, "Reddit Bot", "Failed to reach RedditAPI!");
				// IDK what I want to do here yet.
				// TODO: Be able to output if one specific subreddit was not able to be reached
			}
	}

	public void init()
	{
		run = true;
		if(thread == null || !thread.isAlive())
		{
			thread = new Thread(this);
			thread.start();
		}
	}

	public void endThread()
	{
		run = false;
	}

	private static class CommentData implements INewsData
	{
		private String desc;
		private String comment;

		public CommentData(String desc, String comment)
		{
			this.desc = desc;
			this.comment = comment;
		}

		@Override
		public String getData()
		{
			return comment;
		}

		@Override
		public String getDesc()
		{
			return desc;
		}

		@Override
		public String getServiceID()
		{
			return ScannerCore.SERVICE_ID;
		}

		@Override
		public String getTitle()
		{
			return "Reddit Keywork";
		}

		@Override
		public boolean hasNotification()
		{
			return true;
		}

	}
}
