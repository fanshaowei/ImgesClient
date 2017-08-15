package cn.com.chengzi.ImgesClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * 本地图片上传工具
 * @author fanshaowei
 *
 */
public class App 
{			
	private static final String CONFIG = "config.properties";
	private static String imgSourcePath = null;
	private static String imgCopyPath = null;
	private static String uploadImgUrl = null;
	private static long period = 0;
	
	public static void main(String[] args) {
		getConfigProperties();
		
		final Timer timer = new Timer();
		timer.schedule(new TimerTask() {					
			@Override
			public void run() {
				try {
					getImgStr();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, 5000, period);
		
	}
	
	public static void getConfigProperties(){
		String jarPath = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		String[] fileLevel = jarPath.split("/");
		fileLevel[fileLevel.length - 1] = CONFIG;
		
		StringBuilder sb = new StringBuilder();
		for (String s : fileLevel) {			
			if (s != null && !"".equals(s)) {
				sb.append("/").append(s);
			}else{
				continue;
			}
		}
		jarPath = sb.toString();
		
		Properties properties = new Properties();
		try {
			InputStream inputstream = new BufferedInputStream(new FileInputStream(jarPath));
			properties.load(inputstream);
			
			uploadImgUrl = properties.getProperty("UploadImgUrl");
			imgSourcePath = properties.getProperty("ImgSourcePath");
			imgCopyPath = properties.getProperty("ImgCopyPath");
			period = Long.parseLong(properties.getProperty("Period"));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public static String getImgStr() throws InterruptedException{
		Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
		int year = c.get(Calendar.YEAR); 
		int month = c.get(Calendar.MONTH); 
		int date = c.get(Calendar.DATE); 
		int hour = c.get(Calendar.HOUR_OF_DAY); 
		int minute = c.get(Calendar.MINUTE); 
		int second = c.get(Calendar.SECOND); 
		System.out.println(year + "/" + month + "/" + date + " " +hour + ":" +minute + ":" + second + " 检查新图片");
		
		File fileFload = new File(imgSourcePath);
		File[] files = fileFload.listFiles();
		File fileTemp = null;
		
		for(int i=0; i<files.length; i++){
			fileTemp = files[i];
			String imgName = fileTemp.getName();
			
			try {				
				BufferedOutputStream bufferOutputStream = new BufferedOutputStream(new FileOutputStream(imgCopyPath + File.separator + imgName));				
				BufferedInputStream bufferInputStream = new BufferedInputStream(new FileInputStream(fileTemp));				
				byte[] buff = new byte[bufferInputStream.available()];
				int byteRead;							
				String imgBase64Str = null;
				String isUploadOk = null;
				
				while((byteRead = bufferInputStream.read(buff, 0, buff.length)) != -1){
					imgBase64Str = Base64.encodeBase64String(buff);	//将图片转成BASE64字符串

					isUploadOk = doPost(uploadImgUrl,  imgName + ";" + imgBase64Str);					
					if("true".equals(isUploadOk)){
						System.out.println("upload img success");
						bufferOutputStream.write(buff, 0, byteRead);//复制图片到另一个文件夹								
					}else{
						System.out.println("upload img fail,try again!!!");
						doPost(uploadImgUrl,imgBase64Str);//如果不成功，再传
					}																
				}									
				bufferOutputStream.flush();
				
				bufferOutputStream.close();
				bufferInputStream.close();

				if(fileTemp.exists()){
					fileTemp.delete();//删除原文件					
				}								
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "";
		
	}
	
	
	public static String doPost(String uri, String str){
		HttpPost httpPost = new HttpPost(uri);
		
		//httpPost.addHeader("Content-Type", "text/html");
		StringEntity entity = new StringEntity(str, "UTF-8");				
		httpPost.setEntity(entity);			
		
		return sendHttpRequest(httpPost);
	}
	
	/**
	 * 发送请求
	 * @param request
	 * @return
	 */
	public static String sendHttpRequest(HttpUriRequest request){			
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		String responseContent = null;
		
		try{
			httpClient = HttpClients.createDefault();
			response = httpClient.execute(request);
			entity = response.getEntity();
			responseContent = EntityUtils.toString(entity, "UTF-8");
		}catch(Exception ex){
		    System.out.println(request.getURI().toString() + "请求失败");
			ex.printStackTrace();			
		}finally{			
			try {
				if(response != null)
					response.close();
				if(httpClient != null)
					httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
		
		return responseContent;
	}
}
