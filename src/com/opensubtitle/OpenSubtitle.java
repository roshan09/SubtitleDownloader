package com.opensubtitle;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfig;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Created by roshan09 on 30/7/17.
 */
public class OpenSubtitle {

    private String username;
    private String password;
    private String language;
    private String useragent;

    private static final Logger LOGGER = Logger.getLogger("com.opensubtitle");
    private FileHandler logFileHandler = null;

    private XmlRpcClient xmlRpcClient = null;
    private String sessionId = null;
    private String movieName = null;
    private String subtitleLink = null;

    private void createLogger()
    {
        // logger setting
        try {
            logFileHandler = new FileHandler(Constants.LOG_FILE_PATH);
            logFileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(logFileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadLoginConfig()
    {
        //load loginConfig.json
        JSONParser parser = new JSONParser();
        try {

            JSONObject object =(JSONObject) parser.parse(new FileReader(Constants.LOGIN_CONFIG_FILE_PATH));
            this.username = object.get(Constants.USERNAME).toString();
            this.password = object.get(Constants.PASSWORD).toString();
            this.language = object.get(Constants.LANGUAGE).toString();
            this.useragent = object.get(Constants.USERAGENT).toString();

            LOGGER.info("\n\tusername is "+this.username+"\n\tpassword is "+this.password
                    +"\n\tlanguage is "+this.language+"\n\tuseragent is "+this.useragent+"\n");

        } catch (IOException e) {
            LOGGER.info("\n\tError in opening loginConfig.json "+e);
            e.printStackTrace();
        } catch (ParseException e) {
            LOGGER.info("\n\tError in opening loginConfig.json "+e);
            e.printStackTrace();
        }
    }

    private void createClient()
    {
        // create a XMLRpcClient
        xmlRpcClient = new XmlRpcClient();
        XmlRpcClientConfigImpl xmlRpcClientConfigImpl = new XmlRpcClientConfigImpl();
        try {
            xmlRpcClientConfigImpl.setServerURL(new URL(Constants.OPEN_SUBTITLE_URL));
        } catch (MalformedURLException e) {
            LOGGER.severe("\n\tXMLRpcClientCongImpl failed "+e);
            e.printStackTrace();
        }
        xmlRpcClient.setConfig(xmlRpcClientConfigImpl);
        LOGGER.info("\n\tXMLRpcClient created");

    }
    public OpenSubtitle()
    {
        createLogger();

        loadLoginConfig();

        createClient();
    }

    public void login() {

        LOGGER.info("\n\tLogin to OpenSubtitle....");
        ArrayList <String> param = new ArrayList<String>();
        param.add(this.username);
        param.add(this.password);
        param.add(this.language);
        param.add(this.useragent);

        Object ret = null;

        try {
            ret = xmlRpcClient.execute(Constants.LOGIN_COMMAND,param);
        } catch (XmlRpcException e) {
            LOGGER.severe("\n\tError in login.."+e);
            e.printStackTrace();
        }

       this.sessionId = ((HashMap) ret).get("token").toString();
       Integer status = Integer.getInteger(((HashMap) ret).get("status").toString());
       String time = ((HashMap) ret).get("seconds").toString();


       LOGGER.info("\n\tsessionID :"+ this.sessionId +"\n\tstatus :"+status+"\n\ttime :"+time);

    }

    public void searchSubtitle(String name) {

        movieName = name;

        HashMap <String,String> subParam2 = new HashMap<String,String>();
        subParam2.put("query",movieName);
        subParam2.put("sublanguageid",language);

        HashMap <String,String> subParam3 = new HashMap<String,String>();
        subParam3.put("limit","10");

        ArrayList <Object> param = new ArrayList<Object>();
        param.add(sessionId);
        param.add(new Object[]{subParam2});
        param.add(subParam3);

        Object ret = null;

        try {
            LOGGER.info("\n\tSearching for subtitles");
            ret = xmlRpcClient.execute(Constants.SEARCH_SUBTITLES_COMMAND,param);
        } catch (XmlRpcException e) {
            LOGGER.severe("Error in Searching subtitles "+e);
            e.printStackTrace();
        }

        Object [] movieCopies =(Object[]) ((HashMap)ret).get("data");
        LOGGER.info("\n\tStatus is "+((HashMap)ret).get("status"));

        Object movie = null;
        int maxCnt=0;
        for (Object movieCopy:movieCopies) {

            HashMap <String,String> movieInfo = (HashMap) movieCopy;

            int subtitleDownloadCount = Integer.parseInt(movieInfo.get("SubDownloadsCnt"));
            if(subtitleDownloadCount > maxCnt)
            {
                movie = (Object) movieInfo;
                maxCnt=subtitleDownloadCount;
            }
        }

        DetailsOfMovie(movie);
    }

    public void DetailsOfMovie(Object movie)
    {
        HashMap <String,String> movieInfo = (HashMap<String, String>) movie;

        subtitleLink = movieInfo.get("SubDownloadLink").toString();
        Iterator iterator = movieInfo.entrySet().iterator();
        StringBuffer tmp = new StringBuffer();
        while(iterator.hasNext())
        {
            Map.Entry pair =(Map.Entry) iterator.next();
            tmp.append("\n\t"+pair.getKey()+"  :  "+pair.getValue());
        }
        LOGGER.info("\n\t"+tmp.toString());
    }

    public void downloadSubtitle() {

        LOGGER.info("\n\tDownloading subtitle...");
        URL subtitleUrl = null;

        try {
            subtitleUrl = new URL(subtitleLink.replace(".gz",""));
        } catch (MalformedURLException e) {
            LOGGER.severe("\n\tError while creating URL "+e);
            e.printStackTrace();
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(subtitleUrl.openStream()));
        } catch (IOException e) {
            LOGGER.severe("\n\tError while creating Bufferreader "+e);
            e.printStackTrace();
        }

        File subtitle =new File(Constants.SUBTITLE_DESTINATION_PATH+movieName+".srt");

        if(subtitle.exists())
            subtitle.delete();

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(subtitle));
        } catch (IOException e) {
            LOGGER.severe("\n\tError while creating Buffer writer "+e);
            e.printStackTrace();
        }

        String line = "";
        try {
            while((line=reader.readLine())!=null)
            {
                writer.write(line+"\n");
            }
        } catch (IOException e) {
            LOGGER.severe("\n\tError in reading buffer "+e);
            e.printStackTrace();
        }

        try {
            reader.close();
            writer.close();
        } catch (IOException e) {
            LOGGER.severe("\n\tError in closing the reader and writer "+e);
            e.printStackTrace();
        }

        LOGGER.info("\n\tSubtitles downloaded successfully...");
    }
}

