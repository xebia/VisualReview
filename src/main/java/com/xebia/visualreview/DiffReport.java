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
import java.io.File;
import java.io.IOException;

public class DiffReport {

  private double percentage;

  private BufferedImage diffImage;

  private BufferedImage maskImage;

  private File before;
  private File after;

  public DiffReport (File before, File after, BufferedImage diffImage, int diffCount) {
    this.before = before;
    this.after = after;
    this.diffImage = diffImage;
    this.percentage = diffCount * 100.0 / (diffImage.getHeight() * diffImage.getWidth());
  }

  public void saveDiff(File path) {
    saveImage(diffImage, path);
  }

  public double getPercentage() {
    if (percentage > 0 && percentage < 0.1) return 0.1;
    else return percentage;
  }

  public File getBefore() {
    return before;
  }

  public File getAfter() {
    return after;
  }

  public BufferedImage getDiffImage() {
    return diffImage;
  }

  public BufferedImage getMaskImage() {
    return maskImage;
  }

  public void setMaskImage(BufferedImage maskImage) {
    this.maskImage = maskImage;
  }

  private void saveImage(BufferedImage image, File outputFile) {
    try {
      outputFile.mkdirs();
      ImageIO.write(image, "png", outputFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
 }

}
