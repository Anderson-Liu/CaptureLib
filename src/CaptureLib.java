import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Enn on 2016/3/19.
 */
public class CaptureLib {

    private static CloseableHttpClient httpClient = HttpClients.createDefault();
    static int actiCount = 0;
    static int totalCount = 0;
    static int countToIgnor = 753;
    static int passwdChange = 0;
    public static void loginLib(String password) throws IOException {

        System.out.println("--------Get Cookie for Login---------");

        int htmlCode = 0;
        String loginUrl = "http://opac.ahau.edu.cn/reader/redr_verify.php";
        HttpPost loginPost = new HttpPost(loginUrl);

        loginPost.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:44.0) Gecko/20100101 Firefox/44.0");
        loginPost.addHeader("Referer", "http://lib.ahau.edu.cn/");



        List<NameValuePair> nvpsLogin = new ArrayList<NameValuePair>();
        nvpsLogin.add(new BasicNameValuePair("select", "cert_no"));
        // nvpsLogin.add(new BasicNameValuePair("returnUrl", ""));
        nvpsLogin.add(new BasicNameValuePair("passwd", password));
        Map<String, String> stuList = LoadData.getStuList();
        String stuName = "";
        String userCode = "";
        for (Map.Entry<String, String> entry : stuList.entrySet()) {
            totalCount++;
            if (totalCount < countToIgnor) {
                System.out.println("跳过第" + totalCount + "个");
                continue;
            }
            System.out.println("这是第" + totalCount + "个");
            userCode = entry.getKey();
            stuName = entry.getValue();
            // stuName = entry.getKey();
            // userCode = entry.getValue();
            nvpsLogin.add(new BasicNameValuePair("number", userCode));

            try {
                String imgCode = ImagePreProcess.getCode(httpClient);
                nvpsLogin.add(new BasicNameValuePair("captcha", imgCode));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("出现异常，跳过" + userCode);
                continue;
            }

            loginPost.setEntity(new UrlEncodedFormEntity(nvpsLogin));

            // 执行Post请求登录
            CloseableHttpResponse loginPostResponse = httpClient.execute(loginPost);

            // 获取登录返回数据
            HttpEntity loginEntity = loginPostResponse.getEntity();
            System.out.println("Response Line: " + loginPostResponse.getStatusLine());
            String loginContent = EntityUtils.toString(loginEntity, "utf-8");
            if (loginPostResponse.getFirstHeader("Location") == null) {
                System.out.println(userCode + "获取不到Location, 密码错误，跳过");
                passwdChange++;
                System.out.println("这是第 " + passwdChange +  " 个更改了密码的，共" + totalCount + "个!");
                continue;
            }
            String location = loginPostResponse.getFirstHeader("Location").getValue();
            htmlCode = loginPostResponse.getStatusLine().getStatusCode();
            String infoContent = "";
            if (htmlCode == HttpStatus.SC_MOVED_TEMPORARILY && location.equals("redr_con.php")) {
                System.out.println(userCode + "未激活...");
                actiCount++;
                System.out.println("将进行激活");
                HashMap<String, String> activeResultMap = activeCount(stuName);
                if (activeResultMap.get("htmlCode") == null) {
                    System.out.println("无法获得htmlCode");
                    continue;
                }
                int getCode = Integer.parseInt(activeResultMap.get("htmlCode"));
                String activeContent = activeResultMap.get("content");
                Document content = Jsoup.parse(activeContent);
                // 解析是否含有“新密码”，有则说明是激活成功
                Elements chPasswd = content.select("form:contains(新密码)");
                boolean isOk = chPasswd.toString().contains("新密码");

                if (200 != getCode || true != isOk) {
                    System.out.println(userCode + "激活失败, 跳过");
                    continue;
                }
                System.out.println("激活成功，开始获取用户信息");
                HashMap<String, String> result = getInfo();
                saveAsFile(userCode, result, actiCount);

            } else if (htmlCode == HttpStatus.SC_MOVED_TEMPORARILY && location.equals("redr_info.php")) {
                System.out.println(userCode + "已激活...");
                System.out.println("将进行数据获取！");
                HashMap<String, String> result = getInfo();
                saveAsFile(userCode, result);
            } else {
                System.out.println("响应数据异常；进行下一个！");
                continue;
            }
        }
    }

    public static HashMap<String, String> getInfo() throws IOException {
        int htmlCode;
        String inforUrl = "http://opac.ahau.edu.cn/reader/redr_info_rule.php";
        HttpGet infoGet = new HttpGet(inforUrl);
        System.out.println("Get information post line: " + infoGet.getRequestLine());
        // 执行Get请求获取详细个人信息
        CloseableHttpResponse infoGetResponse = httpClient.execute(infoGet);
        // 获取得到的个人详细信息
        HttpEntity infoGetEntity = infoGetResponse.getEntity();
        htmlCode = infoGetResponse.getStatusLine().getStatusCode();
        String infoContent = EntityUtils.toString(infoGetEntity, "UTF-8");
        String presentContent = getPresentBook();
        String histContent = getHistoryBook();
        String recommContent = getRecommendBook();
        HashMap<String, String> result = new HashMap<>();
        result.put("UserInfo", infoContent);
        result.put("PresentContent", presentContent);
        result.put("HistContent", histContent);
        result.put("RecommContent", recommContent);
        return result;
    }

    public static HashMap<String, String> activeCount(String stuName) {
        String activeUrl = "http://opac.ahau.edu.cn/reader/redr_con_result.php";
        HttpPost activePost = new HttpPost(activeUrl);
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("name", stuName));
        int htmlCode = 0;
        HashMap<String, String> map = new HashMap<>();
        try {
            // 加上utf-8对于添加中文参数非常重要
            activePost.setEntity(new UrlEncodedFormEntity(pairs, "utf-8"));
            activePost.setHeader("Referer", "http://opac.ahau.edu.cn/reader/redr_con.php");
            CloseableHttpResponse activeResposne = httpClient.execute(activePost);
            String activeContent = EntityUtils.toString(activeResposne.getEntity(), "utf-8");
            htmlCode = activeResposne.getStatusLine().getStatusCode();
            // location = activeResposne.getFirstHeader("location").getValue();
            System.out.println(activeResposne.getStatusLine().getStatusCode());
            map.put("htmlCode", Integer.toString(htmlCode));
            map.put("content", activeContent);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoHttpResponseException e) {
            e.printStackTrace();
            System.out.println("网络无响应");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void analyzeContent(){

    }

    public static void saveAsFile(String userCode, HashMap<String, String> map) throws IOException {
        File writeTarget = new File("ResultData//" + userCode + ".txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(writeTarget));
        HashMap<String, String> result = map;
        String contentType = null;
        String resultContent = null;
        String pre = null;
        for (Map.Entry<String, String> infoEntry : result.entrySet()) {
            contentType = infoEntry.getKey();
            resultContent = infoEntry.getValue();
            pre = "\n\n#######################    " + contentType + "    #####################\n\n";
            out.append(pre);
            out.append(resultContent);
        }
    }

    public static void saveAsFile(String userCode, HashMap<String, String> map, int actiCount) throws IOException {
        File writeTarget = new File("ResultData//" + userCode + ".txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(writeTarget));
        String pre = "\n\n##########    At the time of  " + actiCount + " that being active   ################\n\n";
        out.append(pre);
        saveAsFile(userCode, map);
    }


    public static String getPresentBook() throws IOException {
        String presentUrl = "http://opac.ahau.edu.cn/reader/book_lst.php";
        HttpGet getPresent = new HttpGet(presentUrl);
        CloseableHttpResponse response = httpClient.execute(getPresent);
        String content = EntityUtils.toString(response.getEntity(), "utf-8");
        return content;
    }

    public static String getHistoryBook() {
        String hisUrl = "http://opac.ahau.edu.cn/reader/book_hist.php";
        HttpPost hisBookPost = new HttpPost(hisUrl);
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("para_string", "all"));
        CloseableHttpResponse hisResponse = null;
        String content = null;
        try {
            hisBookPost.setEntity(new UrlEncodedFormEntity(pairs, "utf-8"));
            hisResponse = httpClient.execute(hisBookPost);
            content = EntityUtils.toString(hisResponse.getEntity(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    // 读者荐购
    public static String getRecommendBook(){
        String recommUrl = "http://opac.ahau.edu.cn/reader/asord_lst.php";
        HttpGet getRecomm = new HttpGet(recommUrl);
        String content = null;
        try {
            CloseableHttpResponse recommRespon = httpClient.execute(getRecomm);
            content = EntityUtils.toString(recommRespon.getEntity(), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static void main (String args[]) throws IOException {
        String passwd = "0000";
        CaptureLib.loginLib(passwd);
    }
}
