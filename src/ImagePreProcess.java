/**
 * Created by Enn on 2016/3/19.
 */
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;


public class ImagePreProcess {

    public static int isWhite(int colorInt) {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() > 100) {
            return 1;
        }
        return 0;
    }

    public static int isBlack(int colorInt) {
        Color color = new Color(colorInt);
        if (color.getRed() + color.getGreen() + color.getBlue() <= 100) {
            return 1;
        }
        return 0;
    }

    public static BufferedImage removeBackgroud(String picFile)
            throws Exception {
        BufferedImage img = ImageIO.read(new File(picFile));
        int width = img.getWidth();
        int height = img.getHeight();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (isWhite(img.getRGB(x, y)) == 1) {
                    img.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    img.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        return img;
    }

    public static List<BufferedImage> splitImage(BufferedImage img)
            throws Exception {
        List<BufferedImage> subImgs = new ArrayList<BufferedImage>();
        subImgs.add(img.getSubimage(5, 15, 10, 12));
        // subImgs.add(img.getSubimage(19, 16, 6, 10));
        subImgs.add(img.getSubimage(17, 15, 10, 12));
        subImgs.add(img.getSubimage(29, 15, 10, 12));
        subImgs.add(img.getSubimage(41, 15, 10, 12));
        /*for (int i=0; i<4; i++) {
            ImageIO.write(subImgs.get(i), "JPG", new File("train//gen0"+ i +".jpg"));
        }
*/
        return subImgs;
    }

    public static Map<BufferedImage, String> loadTrainData() throws Exception {
        Map<BufferedImage, String> map = new HashMap<BufferedImage, String>();
        File dir = new File("train");
        File[] files = dir.listFiles();
        for (File file : files) {
            map.put(ImageIO.read(file), file.getName().charAt(0) + "");
        }
        return map;
    }

    public static String getSingleCharOcr(BufferedImage img,
                                          Map<BufferedImage, String> map) {
        String result = "";
        int width = img.getWidth();
        int height = img.getHeight();
        int min = width * height;
        for (BufferedImage bi : map.keySet()) {
            int count = 0;
            Label1: for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (isWhite(img.getRGB(x, y)) != isWhite(bi.getRGB(x, y))) {
                        count++;
                        if (count >= min)
                            break Label1;
                    }
                }
            }
            if (count < min) {
                min = count;
                result = map.get(bi);
            }
        }
        return result;
    }

    public static String getAllOcr(String file) throws Exception {
        BufferedImage img = removeBackgroud(file);
        List<BufferedImage> listImg = splitImage(img);
        Map<BufferedImage, String> map = loadTrainData();
        String result = "";
        for (BufferedImage bi : listImg) {
            result += getSingleCharOcr(bi, map);
        }
        ImageIO.write(img, "JPG", new File("result//"+result+".jpg"));
        return result;
    }

    public static void downloadImage(CloseableHttpClient httpClient) throws IOException {

        String imgUrl = "http://opac.ahau.edu.cn/reader/captcha.php";

        HttpGet imgGet = new HttpGet(imgUrl);
        CloseableHttpResponse imgGetResponse = httpClient.execute(imgGet);
        HttpEntity imgEntity = imgGetResponse.getEntity();

        for (int i = 0; i < 1; i++) {
            try {
                // 执行getMethod
                int statusCode = imgGetResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    System.err.println("Method failed: "
                            + imgGetResponse.getStatusLine());
                }
                // 读取内容
                String picName = "img//" + i + ".jpg";
                InputStream inputStream = imgGetResponse.getEntity().getContent();
                OutputStream outStream = new FileOutputStream(picName);
                // IOUtils.copy(inputStream, outStream);
                byte[] buffer = new byte[1024]; // 创建存放输入流的缓冲
                int num = -1; // 读入的字节数
                while (true)
                {
                    num = inputStream.read(buffer); // 读入到缓冲区
                    if (num == -1)
                    {
                        outStream.flush();
                        break; // 已经读完
                    }
                    outStream.flush();
                    outStream.write(buffer, 0, num);
                }
                outStream.close();
                System.out.println("OK!");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 释放连接
                // EntityUtils.consume(imgEntity);
                imgGet.releaseConnection();
            }
        }
    }

    public static String getCode(CloseableHttpClient httpClient) throws Exception {
        downloadImage(httpClient);
        int i = 0;
        String text = getAllOcr("img//" + i + ".jpg");
        System.out.println(i + ".jpg = " + text);
        return text;
    }
    /**
     * @param args
     * @throws Exception
     *//*
    public static void main(String[] args) throws Exception {
        downloadImage();
        int i = 0;
        String text = getAllOcr("img//" + i + ".jpg");
        System.out.println(i + ".jpg = " + text);

    }*/
}
