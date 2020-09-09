package requests;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;

public class CurrentlyPlaying implements Runnable{

    static AbstractHttpClient httpclient;
    static HttpGet httpGet;
    static ResponseHandler<String> responseHandler;
    static JSONObject array;
    static String artist;

    static Thread update;

    static Thread thread;
    static boolean paused = true;
    static boolean oldPaused = true;

    static boolean running = true;

    public static void main(String[] args) {
        update = new Thread(() -> {
            double target = 1;
            double nsPerTick = 1000000000.0 / target;
            long lastTime = System.nanoTime();
            long timer = System.currentTimeMillis();
            double unprocessed = 0.0;

            while (true) {
                long now = System.nanoTime();
                unprocessed += (now - lastTime) / nsPerTick;
                //System.out.println(unprocessed);
                lastTime = now;

                if(unprocessed >= 5.0){
                    new Thread(() -> update(), "spotify-update").start();
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(System.currentTimeMillis() - 1000 > timer){
                    timer += 1000;
                }
            }
        });
        update.start();
        thread = new Thread(new CurrentlyPlaying(), "spotify-api");
        update();
    }



    public static void update() {
        httpclient = new DefaultHttpClient();
        httpGet = new HttpGet("https://api.spotify.com/v1/me/player/currently-playing");
        httpGet.addHeader("Accept", "application/json");
        httpGet.addHeader("Content-Type", "application/json");
        httpGet.addHeader("Authorization", "");
        responseHandler = new BasicResponseHandler();
        String responseBody = null;
        try {
            responseBody = httpclient.execute(httpGet, responseHandler);
            array = new JSONObject(responseBody);
            String oldArtists = artist;
            artist = array.getJSONObject("item").getJSONArray("artists").getJSONObject(0).getString("name");
            for (int i = 0; i < array.getJSONObject("item").getJSONArray("artists").length(); i++) {
                if(i == 0) continue;
                artist = artist + ", " + array.getJSONObject("item").getJSONArray("artists").getJSONObject(i).getString("name");
            }
            if(!artist.equals(oldArtists)) {
                if(thread.isAlive()) thread.stop();
                thread = new Thread(new CurrentlyPlaying(), "spotify-api");
                thread.start();
            }
            oldPaused = paused;
            paused = array.getBoolean("is_playing");
            if(oldPaused != paused) {
                if(paused == true) {
                    running = false;
                    Thread.sleep(100);
                    thread = new Thread(new CurrentlyPlaying(), "spotify-api");
                    running = true;
                    thread.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            System.out.print("\n");
            System.out.println("-----------------------------------------------");
            System.out.println(array.getJSONObject("item").getString("name") + " - " + artist);
            SimpleDateFormat format = new SimpleDateFormat("mm:ss");
            int target = 1;
            double nsPerTick = 1000000000.0 / target;;
            long lastTime = System.nanoTime();
            long timer = System.currentTimeMillis();
            double unprocessed = 0.0;
            long i = array.getLong("progress_ms") + 1000;
            while (running) {
                long now = System.nanoTime();
                unprocessed += (now - lastTime) / nsPerTick;
                //System.out.println(unprocessed);
                lastTime = now;

                if(unprocessed >= 1.0){
                    if(paused) {
                        int percent = (int) (i * 100 / array.getJSONObject("item").getLong("duration_ms"));
                        StringBuilder string = new StringBuilder();
                        string.append("\r")
                                .append("[")
                                .append(String.join("", Collections.nCopies(percent, "=")))
                                .append('>')
                                .append(String.join("", Collections.nCopies(100 - percent, " ")))
                                .append("] ")
                                .append(format.format(i))
                                .append(" - ")
                                .append(format.format(array.getJSONObject("item").getLong("duration_ms")));
                        System.out.print(string);
                        i+=1000;
                    }
                    unprocessed--;
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(System.currentTimeMillis() - 1000 > timer){
                    timer += 1000;
                }
                /*if(i > array.getJSONObject("item").getLong("duration_ms")) {
                    break;
                }*/
            }

        } finally {
            //update();
        }
    }
}
