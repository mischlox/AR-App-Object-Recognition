/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package hs.aalen.arora;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.view.Display;
import android.view.Surface;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/** Utility class for manipulating images.
 *
 * The functions are taken from TFLite object detection example
 * and sometimes slightly adjusted for this application
 */
public class ImageUtils {

  // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
  // are normalized to eight bits.
  static final int MAX_CHANNEL_VALUE = 262143;
  static final int LOWER_BYTE_MASK = 0xFF;


  private static int yuv2Rgb(int y, int u, int v) {
    // Adjust and check YUV values
    y = (y - 16) < 0 ? 0 : (y - 16);
    u -= 128;
    v -= 128;

    // This is the floating point equivalent. We do the conversion in integer
    // because some Android devices do not have floating point in hardware.
    // nR = (int)(1.164 * nY + 2.018 * nU);
    // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
    // nB = (int)(1.164 * nY + 1.596 * nV);
    int y1192 = 1192 * y;
    int r = (y1192 + 1634 * v);
    int g = (y1192 - 833 * v - 400 * u);
    int b = (y1192 + 2066 * u);

    // Clipping RGB values to be inside boundaries [ 0 , MAX_CHANNEL_VALUE ]
    r = r > MAX_CHANNEL_VALUE ? MAX_CHANNEL_VALUE : Math.max(r, 0);
    g = g > MAX_CHANNEL_VALUE ? MAX_CHANNEL_VALUE : Math.max(g, 0);
    b = b > MAX_CHANNEL_VALUE ? MAX_CHANNEL_VALUE : Math.max(b, 0);

    return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
  }

  public static void convertYUV420ToARGB8888(
      byte[] yData,
      byte[] uData,
      byte[] vData,
      int width,
      int height,
      int yRowStride,
      int uvRowStride,
      int uvPixelStride,
      int[] out) {
    int yp = 0;
    for (int j = 0; j < height; j++) {
      int pY = yRowStride * j;
      int pUV = uvRowStride * (j >> 1);

      for (int i = 0; i < width; i++) {
        int uvOffset = pUV + (i >> 1) * uvPixelStride;

        out[yp++] = yuv2Rgb(0xff & yData[pY + i], 0xff & uData[uvOffset], 0xff & vData[uvOffset]);
      }
    }
  }

  /**
   * Takes an ImageProxy object in YUV format
   * and converts it into a RGB Bitmap
   *
   * @param imageProxy the image that will be converted
   * @return the converted image as a Bitmap
   */
  public static Bitmap yuvCameraImageToBitmap(ImageProxy imageProxy) throws NullPointerException {
    if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
      throw new IllegalArgumentException(
              "Expected a YUV420 image, but got " + imageProxy.getFormat());
    }

    ImageProxy.PlaneProxy yPlane = imageProxy.getPlanes()[0];
    ImageProxy.PlaneProxy uPlane = imageProxy.getPlanes()[1];

    int width = imageProxy.getWidth();
    int height = imageProxy.getHeight();

    byte[][] yuvBytes = new byte[3][];
    int[] argbArray = new int[width * height];
    for (int i = 0; i < imageProxy.getPlanes().length; i++) {
      final ByteBuffer buffer = imageProxy.getPlanes()[i].getBuffer();
      yuvBytes[i] = new byte[buffer.capacity()];
      buffer.get(yuvBytes[i]);
    }

    ImageUtils.convertYUV420ToARGB8888(
            yuvBytes[0],
            yuvBytes[1],
            yuvBytes[2],
            width,
            height,
            yPlane.getRowStride(),
            uPlane.getRowStride(),
            uPlane.getPixelStride(),
            argbArray);

    return Bitmap.createBitmap(argbArray, width, height, Bitmap.Config.ARGB_8888);
  }

  /**
   * Normalizes a camera image to [0 ; 1] and crops it
   * to the size that is expected by the model
   *
   * @param bitmap          The converted image that will be normalized and cropped
   * @param rotationDegrees Handles landscape/portrait mode with post rotation if necessary
   * @return the cropped and normalized image
   */
  public static float[] prepareCameraImage(Bitmap bitmap, int rotationDegrees, int[] cropLocations) {
    int modelImageSize = TransferLearningModelWrapper.IMAGE_SIZE;
    Bitmap rotatedBitmap = scaleAndRotateBitmap(bitmap, rotationDegrees, modelImageSize, cropLocations);

    float[] normalizedRgb = new float[modelImageSize * modelImageSize * 3];
    int nextIdx = 0;
    for (int y = 0; y < modelImageSize; y++) {
      for (int x = 0; x < modelImageSize; x++) {
        int rgb = rotatedBitmap.getPixel(x, y);

        float r = ((rgb >> 16) & LOWER_BYTE_MASK) * (1 / 255.f);
        float g = ((rgb >> 8) & LOWER_BYTE_MASK) * (1 / 255.f);
        float b = (rgb & LOWER_BYTE_MASK) * (1 / 255.f);

        normalizedRgb[nextIdx++] = r;
        normalizedRgb[nextIdx++] = g;
        normalizedRgb[nextIdx++] = b;
      }
    }

    return normalizedRgb;
  }

  public static Bitmap scaleAndRotateBitmap(Bitmap bitmap, int rotationDegrees, int modelImageSize, int[] cropLocations) {

    Bitmap paddedBitmap = padToSquare(bitmap, cropLocations);
    Bitmap scaledBitmap = Bitmap.createScaledBitmap(
            paddedBitmap, modelImageSize, modelImageSize, true);

    Matrix rotationMatrix = new Matrix();
    rotationMatrix.postRotate(rotationDegrees);
    return Bitmap.createBitmap(
            scaledBitmap, 0, 0, modelImageSize, modelImageSize, rotationMatrix, false);
  }

  /**
   * Crop image into square format
   *
   * @param source image to be cropped
   * @return cropped image
   */
  private static Bitmap padToSquare(Bitmap source, int[] cropLocations) {
    // Crop the image to center
    source = Bitmap.createBitmap(source, cropLocations[0], cropLocations[1], cropLocations[2]-cropLocations[0], cropLocations[3]-cropLocations[1]);
    int width = source.getWidth();
    int height = source.getHeight();
    // Make it square
    int paddingX = width < height ? (height - width) / 2 : 0;
    int paddingY = height < width ? (width - height) / 2 : 0;
    Bitmap paddedBitmap = Bitmap.createBitmap(
            (width + 2 * paddingX), (height + 2 * paddingY), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(paddedBitmap);
    canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF);
    canvas.drawBitmap(source, paddingX, paddingY, null);
    return paddedBitmap;
  }

  /**
   * Get the display rotation
   *
   * @param display android display
   * @return rotation value
   */
  public static Integer getDisplaySurfaceRotation(Display display) {
    if (display == null) {
      return null;
    }

    switch (display.getRotation()) {
      case Surface.ROTATION_0:
        return 0;
      case Surface.ROTATION_90:
        return 90;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_270:
        return 270;
      default:
        return null;
    }
  }
}
