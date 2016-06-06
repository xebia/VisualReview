/*
 * Copyright 2015 Xebia B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xebia.visualreview;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;
import java.awt.Color;
import java.util.logging.Handler;

import clojure.lang.Keyword;
import org.json.JSONArray;
import org.json.JSONObject;

public class PixelComparator {

    private static int DIFFCOLOUR = (128<<24) & 0xff000000|       //a
            (128<<16) &   0xff0000|       //r
            (0  <<8 ) &     0xff00|       //g
            (0  <<0 ) &       0xff;       //b

    private static class Rect{
        private int x;
        private int y;
        private int width;
        private int height;
        public boolean isValid;
        public Rect(int x,int y,int width, int height){
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            isValid = true;
        }
        public Rect(java.util.Map info, int maxWidth, int maxHeight){
            isValid = true;
            try{
                int x = Integer.parseInt(info.get(Keyword.find("x")).toString());
                int y = Integer.parseInt(info.get(Keyword.find("y")).toString());
                int width = Integer.parseInt(info.get(Keyword.find("width")).toString());
                int height = Integer.parseInt(info.get(Keyword.find("height")).toString());

                this.x = Math.min(Math.max(0,x),maxWidth);
                this.y = Math.max(Math.min(this.y,maxHeight),y);
                this.width = Math.min(Math.max(0,width),maxWidth-this.x);
                this.height = Math.min(Math.max(0,height),maxHeight-this.y);
            }catch (Exception e) {
                System.out.println("Invalid rect "+info);
                isValid = false;
            }
        }
/*
        public Rect(JSONObject info, int maxWidth, int maxHeight){
            isValid = true;
            try{
                this.x = Math.min(Math.max(0,info.getInt("x")),maxWidth);
                this.y = Math.max(Math.min(this.y,maxHeight),info.getInt("y"));
                this.width = Math.min(Math.max(0,info.getInt("width")),maxWidth-this.x);
                this.height = Math.min(Math.max(0,info.getInt("height")),maxHeight-this.y);
            }catch (Exception e) {
                System.out.println("Invalid rect "+info);
                isValid = false;
            }
        }*/

        public void applyToImage(BufferedImage image,int rgb){
            if (isValid){
                for(int i = x;i<x+width;i++){
                    for(int j = y;j<y+height;j++){
                        image.setRGB(i,j,rgb);
                    }
                }
            }
        }
    }

    private static BufferedImage generateMask(java.util.Map maskInfo, int width, int height){
        BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Rect fullRect = new Rect(0,0,width,height);
        //fullRect.applyToImage(maskImage,Color.WHITE.getRGB());
        if (maskInfo != null) {
            //parse json string info to Map
            java.util.List<java.util.Map> exludeszones = (java.util.List<java.util.Map>)maskInfo.get(Keyword.find("exludeZones"));
            for (int i = 0;i<exludeszones.size();i++){
                java.util.Map exludeZone = exludeszones.get(i);
                Rect rect = new Rect(exludeZone,width,height);
                rect.applyToImage(maskImage,DIFFCOLOUR);
            }
            /*
            for (HashMap<String, Integer> jsonRect : exludeszones) {
                Rect rect = new Rect(jsonRect,width,height);
                rect.applyToImage(maskImage,DIFFCOLOUR);
            }*/
            /*
            try{
                JSONObject object = new JSONObject(maskInfo);
                System.out.println("" +object);

            }catch(Exception e){
                System.out.println("Invalid mask "+maskInfo);
                throw new RuntimeException(e);
            }*/
        }
        return maskImage;
    }

    public static DiffReport processImage(File beforeFile, File afterFile ,java.util.Map maskInfo) {
        System.out.println("Mask received "+maskInfo);
        try {
            PixelGrabber beforeGrab = grabImage(beforeFile);
            PixelGrabber afterGrab = grabImage(afterFile);

            int[] beforeData = null;
            int[] afterData = null;
            int beforeWidth = 0;
            int afterWidth = 0;
            int y1 = 0;
            int y2 = 0;

            if (beforeGrab.grabPixels()) {
                beforeWidth = beforeGrab.getWidth();
                y1 = beforeGrab.getHeight();
                beforeData = (int[]) beforeGrab.getPixels();
            }

            if (afterGrab.grabPixels()) {
                afterWidth = afterGrab.getWidth();
                y2 = afterGrab.getHeight();
                afterData = (int[]) afterGrab.getPixels();
            }

            int minX = Math.min(beforeWidth, afterWidth);
            int minY = Math.min(y1, y2);
            int diffWidth = Math.max(beforeWidth, afterWidth);
            int diffHeight = Math.max(y1, y2);
            int[] diffData = new int[diffWidth * diffHeight];
            int differentPixels = 0;
            boolean hasMask =  (maskInfo != null) ;// metaInfo.containsKey(Keyword.find("mask"));
            //BufferedImage maskImage = generateMask((String)metaInfo.get(Keyword.find("mask")),diffWidth,diffHeight);
            BufferedImage maskImage = generateMask(maskInfo,diffWidth,diffHeight);
            for (int y = 0; y < diffHeight; y++) {
                for (int x = 0; x < diffWidth; x++) {
                    if (maskImage.getRGB(x,y) != DIFFCOLOUR/*Color.WHITE.getRGB()*/) {
                        if (x >= minX || y >= minY || beforeData[y * beforeWidth + x] != afterData[y * afterWidth + x]) {
                            diffData[y * diffWidth + x] = DIFFCOLOUR;
                            differentPixels++;
                           //;
                        }
                    }else {
                        //System.out.println("inside mask, pixel ignored: "+x+","+y);
                    }
                }
            }

            BufferedImage diffImage = new BufferedImage(diffWidth, diffHeight, BufferedImage.TYPE_INT_ARGB);
            diffImage.setRGB(0, 0, diffWidth, diffHeight, diffData, 0, diffWidth);

            DiffReport report = new DiffReport(beforeFile, afterFile, diffImage, differentPixels);
            if (hasMask){
                report.setMaskImage(maskImage);
            }
            return report;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage loadImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static PixelGrabber grabImage(File file) {
        try {
            return new PixelGrabber(loadImage(file), 0, 0, -1, -1, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
