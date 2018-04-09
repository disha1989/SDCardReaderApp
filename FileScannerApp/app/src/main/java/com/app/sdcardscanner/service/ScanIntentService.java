package com.app.sdcardscanner.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by disha on 4/8/18.
 * <p>
 * Intent service to scan the sd card files
 */

public class ScanIntentService extends IntentService {
    public static final String TOP10 = "topFileSize10";
    public static final String AVERAGE = "averageSize";
    public static final String FREQUENTEXT = "extFrequent5";
    public static final String NOTIFICATION = "com.app.sdcardscanner";
    private final String state = Environment.getExternalStorageState();
    Map<String, Integer> files = new HashMap<>();
    Map<String, Double> nameSize = new HashMap<>();
    Map<String, Double> sorted_nameSize = new HashMap<>();
    Map<String, Integer> mostFrequent = new HashMap<>();
    private int fileCount = 0;
    private String top10;
    private String average;
    private String frequent5;

    public ScanIntentService() {
        super("ScanIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

        }

        if (Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {  // we can read the External Storage...
            String SdPATH = Environment.getExternalStorageDirectory().getPath();
            long sdCardSize = Environment.getExternalStorageDirectory().getTotalSpace();
            File filesPath = new File(SdPATH);
            scanSDCard(filesPath, sdCardSize, intent);

        }

        top10 = getTop10();
        average = getAverageFileSize();
        frequent5 = getMostFrequentFileExtension();

        Intent broadcastIntent = new Intent(NOTIFICATION);
        broadcastIntent.putExtra(TOP10, top10);
        broadcastIntent.putExtra(AVERAGE, average);
        broadcastIntent.putExtra(FREQUENTEXT, frequent5);
        sendBroadcast(broadcastIntent);

    }

    public void scanSDCard(File dir, long totalSize, Intent intent) {

        String extension = "";
        try {
            Log.i("ABSOLUTE_PATH", dir.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dir.exists()) {
            long fileSize = 0;
            File[] listFile = dir.listFiles();

            if (listFile != null) {
                for (File aListFile : listFile) {
                    if (aListFile.isDirectory()) {
                        scanSDCard(aListFile, totalSize, intent);
                    } else if (aListFile.isFile()) {
                        fileSize = fileSize + aListFile.getTotalSpace();
                        fileCount++;
                        int in = aListFile.getName().lastIndexOf('.');
                        String name = aListFile.getName();
                        double size = aListFile.length() / (1024);
                        nameSize.put(name, size);
                        if (in >= 0 && name.length() > in + 1) {
                            try {
                                //extension = name.substring(i + 1); //get extension type
                                extension = name.substring(in + 1);
                                if (!files.containsKey(extension)) {
                                    files.put(extension, 1);
                                } else {
                                    Integer te = files.get(extension);
                                    files.put(extension, te + 1);
                                }
                            } catch (IndexOutOfBoundsException e) {
                                System.out.println(name);
                                fileCount--;
                            }

                        }
                        //calculate percentage for progress bar
                        Bundle bundle = intent.getExtras();
                        if (bundle != null) {
                            Messenger messenger = (Messenger) bundle.get("messenger");
                            Message msg = Message.obtain();
                            Bundle data = new Bundle(1);
                            int percentage = (int) ((fileSize * 100) / totalSize);
                            data.putInt("percentage", percentage);
                            msg.setData(data); //put the data here
                            try {
                                assert messenger != null;
                                messenger.send(msg);
                            } catch (RemoteException e) {
                                Log.i("error", "error");
                            }
                        }

                    }
                }
            }
        }
    }

    public String getTop10() {

        String temp = "";
        sorted_nameSize = sortBySize((HashMap<String, Double>) nameSize);
        int size = sorted_nameSize.size();
        System.out.println("---------------------------------------" + size);
        int ten = 0;
        Iterator it = sorted_nameSize.entrySet().iterator();
        while (it.hasNext() && ten <= 10) {
            System.out.println("----------**************------------------" + ten);
            ten++;
            System.out.println("****************************************" + ten);

            System.out.println("--------------^^^^^^^^^^^^^------------" + size);
            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^" + ten);
            Map.Entry pair = (Map.Entry) it.next();
            temp = temp.concat(pair.getKey() + " - " + pair.getValue() + " kilobytes\n");
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove();

        }

        return temp;

    }

    public String getAverageFileSize() {
        String temp1 = "";
        double average = 0;
        Iterator it = nameSize.entrySet().iterator();
        for (int i = 1; it.hasNext(); i++) {
            Map.Entry pair = (Map.Entry) it.next();
            average = (i - 1) * average / i + ((double) pair.getValue() / i);
            it.remove(); // avoids a ConcurrentModificationException
        }
        temp1 = formatBytes(average);
        return temp1;
    }

    private String formatBytes(double bytes) {
        if (bytes < 1024) { return bytes + " Bytes"; } else if (bytes < 1048576) {
            return (Math.round(bytes / 1024)) + " KB";
        } else if (bytes < 1073741824) {
            return (Math.round(bytes / 1048576)) + " MB";
        } else { return (Math.round(bytes / 1073741824)) + " GB"; }
    }

    public String getMostFrequentFileExtension() {

        String temp3 = "";
        mostFrequent = sortByFrequency((HashMap<String, Integer>) files);
        //int size = mostFrequent.size();
        int five = 1;
        Iterator it = mostFrequent.entrySet().iterator();
        while (it.hasNext() && five <= 5) {

            five++;
            Map.Entry pair = (Map.Entry) it.next();
            temp3 = temp3.concat(
                    "Extension (" + pair.getKey() + ") " + " ----- " + "Repeated " + pair.getValue() + " times\n");
            System.out.println(pair.getKey() + " = " + pair.getValue());
            it.remove();

            // avoids a ConcurrentModificationException
        }

        return temp3;
    }

    public HashMap<String, Double> sortBySize(HashMap<String, Double> hmap) {

        Set<Map.Entry<String, Double>> entries = hmap.entrySet();
        Comparator<Map.Entry<String, Double>> valueComparator = new Comparator<Map.Entry<String, Double>>() {

            @Override
            public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
                Double v1 = e1.getValue();
                Double v2 = e2.getValue();
                return v2.compareTo(v1);
            }
        };

        // Sort method needs a List, so let's first convert Set to List in Java
        List<Map.Entry<String, Double>> listOfEntries = new ArrayList<>(entries);

        // sorting HashMap by values using comparator
        Collections.sort(listOfEntries, valueComparator);

        LinkedHashMap<String, Double> sortedByValue = new LinkedHashMap<>(listOfEntries.size());

        // copying entries from List to Map
        for (Map.Entry<String, Double> entry : listOfEntries) {
            sortedByValue.put(entry.getKey(), entry.getValue());
        }

        System.out.println("HashMap after sorting entries by values ");
        Set<Map.Entry<String, Double>> entrySetSortedByValue = sortedByValue.entrySet();

        for (Map.Entry<String, Double> mapping : entrySetSortedByValue) {
            System.out.println(mapping.getKey() + " ==> " + mapping.getValue());
        }

        return sortedByValue;
    }

    public HashMap<String, Integer> sortByFrequency(HashMap<String, Integer> hmap) {

        Set<Map.Entry<String, Integer>> entries = hmap.entrySet();
        Comparator<Map.Entry<String, Integer>> valueComparator = new Comparator<Map.Entry<String, Integer>>() {

            @Override
            public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                Integer v1 = e1.getValue();
                Integer v2 = e2.getValue();
                return v2.compareTo(v1);
            }
        };

        // Sort method needs a List, so let's first convert Set to List in Java
        List<Map.Entry<String, Integer>> listOfEntries = new ArrayList<>(entries);

        // sorting HashMap by values using comparator
        Collections.sort(listOfEntries, valueComparator);

        LinkedHashMap<String, Integer> sortedByValue = new LinkedHashMap<>(listOfEntries.size());

        // copying entries from List to Map
        for (Map.Entry<String, Integer> entry : listOfEntries) {
            sortedByValue.put(entry.getKey(), entry.getValue());
        }

        System.out.println("HashMap after sorting entries by values ");
        Set<Map.Entry<String, Integer>> entrySetSortedByValue = sortedByValue.entrySet();

        for (Map.Entry<String, Integer> mapping : entrySetSortedByValue) {
            System.out.println(mapping.getKey() + " ==> " + mapping.getValue());
        }
        return sortedByValue;
    }

}
