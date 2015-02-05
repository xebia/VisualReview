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

public class PixelComparator {

  private static int DIFFCOLOUR = (128<<24) & 0xff000000|       //a
                                  (128<<16) &   0xff0000|       //r
                                  (0  <<8 ) &     0xff00|       //g
                                  (0  <<0 ) &       0xff;       //b

  public static DiffReport processImage(File beforeFile, File afterFile) {
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

      for (int y = 0; y < diffHeight; y++) {
        for (int x = 0; x < diffWidth; x++) {
          if (x >= minX || y >= minY || beforeData[y * beforeWidth + x] != afterData[y * afterWidth + x]) {
              diffData[y * diffWidth + x] = DIFFCOLOUR;
              differentPixels++;
          }
        }
      }

      BufferedImage diffImage = new BufferedImage(diffWidth, diffHeight, BufferedImage.TYPE_INT_ARGB);
      diffImage.setRGB(0, 0, diffWidth, diffHeight, diffData, 0, diffWidth);

      return new DiffReport(beforeFile, afterFile, diffImage, differentPixels);

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
