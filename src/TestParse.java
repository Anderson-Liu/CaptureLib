import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Enn on 2016/3/20.
 */
public class TestParse {
    public static void main(String args[]) {
        parsHist();
    }

    public static void parsHist() {
        Map<String, String> stuList = LoadData.getStuList();
        String userCode = null;
        String userName = null;
        for (Map.Entry<String, String> entry : stuList.entrySet()) {
            userCode = entry.getKey();
            userName = entry.getValue();
            String pathName = "FinalResult//成绩优秀学生//" + userCode + ".txt";
            File file = new File(pathName);
            FileInputStream reader = null;
            try {
                reader = new FileInputStream(file);
                String encoding = "utf-8";
                Long fileLength = file.length();
                byte[] fileContent = new byte[fileLength.intValue()];
                reader.read(fileContent);
                String content = new String(fileContent, encoding);
                reader.close();

                Document document = Jsoup.parse(content);

                // 解析借阅的书名，作者，借书,还书时间和馆藏地址
                ArrayList bookName = new ArrayList();
                ArrayList authorName= new ArrayList();
                int bookCount = bookName.size();
                Elements whiteText = document.getElementsByClass("whitetext");
                ArrayList borrowDate = new ArrayList();
                ArrayList returnDate = new ArrayList();
                ArrayList bookArea = new ArrayList();
                for (int i = 0; i < bookCount; i++) {
                    bookName.add(whiteText.get(i * 7 + 2).text());
                    authorName.add(whiteText.get(i * 7 + 3).text().split(" ")[0]);
                    borrowDate.add(whiteText.get(i * 7 + 4).text());
                    returnDate.add(whiteText.get(i * 7 + 5).text());
                    bookArea.add(whiteText.get(i * 7 + 6).text());
                }

                String tmp = document.select("h2:contains(荐购历史)").get(0).text();
                String regEx="[^0-9]";
                Pattern p = Pattern.compile(regEx);
                Matcher m = p.matcher(tmp);
                int recommCount = Integer.parseInt(m.replaceAll("").trim());
                if (recommCount > 0) {

                } else {
                    System.out.println("该读者没有推荐书籍");
                }

                // 不完整数据的检测与处理
                Element bluetext = document.getElementById("mylib_info");
                String totalBorrow = bluetext.select("td:contains(累计借书)").text();

                String readerType = bluetext.select("td:contains(读者类型)").text().split("：")[1];
                String peccancy = bluetext.select("td:contains(违章次数)").text().split("：")[1];
                String department = bluetext.select("td:contains(系别)").text().split("：")[1];
                System.out.println(recommCount);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
