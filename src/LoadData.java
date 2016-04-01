import java.io.*;
import java.util.*;

/**
 * Created by Enn on 2016/3/20.
 */
public class LoadData {
    public static Map<String, String> getStuList() {

        HashMap<String, String> map = new HashMap<>();
        ArrayList stuList = new ArrayList();
        try {
            String line = null;
            String pathname = "数据//成绩优秀学生.txt";
            File file = new File(pathname);
            InputStreamReader reader = null;
            reader = new InputStreamReader(
                    new FileInputStream(file));
            // BufferedReader br = new BufferedReader(reader);
            Scanner scanner = new Scanner(file);
            String[] namePair = {};
            while (scanner.hasNext()) {
                line = scanner.nextLine();
                if (!line.isEmpty()) {
                    namePair = line.split("\t");
                    if (namePair.length >= 2) {
                        map.put(namePair[0], namePair[1]);
                        stuList.add(namePair);
                    }
                }
            }
            System.out.println(namePair.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void main(String args[]) {
        getStuList();
    }
}


