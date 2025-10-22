/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.tests.images;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.junit.jupiter.api.Test;

/**
 * Tests for URLHintProvider functionality that detects desired image sizes from
 * URL paths and query parameters.
 *
 * <p>
 * Note: SVG size hint support requires SWT 3.132+ with native SVG loading
 * capabilities. Tests verify that hints are detected and passed to the loader,
 * but actual rendering depends on platform SVG support.
 * </p>
 */
public class URLHintProviderTest {

	@Test
	public void testPathBasedHintDetection16x16() throws Exception {
		URL url = URLHintProviderTest.class.getResource("/icons/imagetests/16x16/test-icon.svg");
		assertNotNull(url, "Test SVG not found");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(100);

		assertNotNull(imageData, "ImageData should not be null");
		// If SVG size hints are supported, should be 16x16, otherwise native 128x128
		assertEquals(16, imageData.width, "Width should be 16 based on path hint");
		assertEquals(16, imageData.height, "Height should be 16 based on path hint");
	}

	@Test
	public void testPathBasedHintDetection32x32() throws Exception {
		URL url = URLHintProviderTest.class.getResource("/icons/imagetests/32x32/test-icon.svg");
		assertNotNull(url, "Test SVG not found");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(100);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(32, imageData.width, "Width should be 32 based on path hint");
		assertEquals(32, imageData.height, "Height should be 32 based on path hint");
	}

	@Test
	public void testPathBasedHintDetectionZoom200() throws Exception {
		URL url = URLHintProviderTest.class.getResource("/icons/imagetests/16x16/test-icon.svg");
		assertNotNull(url, "Test SVG not found");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(200);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(32, imageData.width, "Width should be 32 (16*2) at 200% zoom");
		assertEquals(32, imageData.height, "Height should be 32 (16*2) at 200% zoom");
	}

	@Test
	public void testPathBasedHintDetectionZoom150() throws Exception {
		URL url = URLHintProviderTest.class.getResource("/icons/imagetests/16x16/test-icon.svg");
		assertNotNull(url, "Test SVG not found");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(150);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(24, imageData.width, "Width should be 24 (16*1.5) at 150% zoom");
		assertEquals(24, imageData.height, "Height should be 24 (16*1.5) at 150% zoom");
	}

	@Test
	public void testQueryParameterHintDetection() throws Exception {
		URL baseUrl = URLHintProviderTest.class.getResource("/icons/imagetests/test-icon.svg");
		assertNotNull(baseUrl, "Test SVG not found");

		// Create URL with query parameter - using jar: protocol from class loader
		String urlString = baseUrl.toExternalForm();
		URL url = new URL(urlString + "?size=16x16");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(100);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(16, imageData.width, "Width should be 16 based on query parameter");
		assertEquals(16, imageData.height, "Height should be 16 based on query parameter");
	}

	@Test
	public void testQueryParameterHintDetection64x64() throws Exception {
		URL baseUrl = URLHintProviderTest.class.getResource("/icons/imagetests/test-icon.svg");
		assertNotNull(baseUrl, "Test SVG not found");

		String urlString = baseUrl.toExternalForm();
		URL url = new URL(urlString + "?size=64x64");
		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(100);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(64, imageData.width, "Width should be 64 based on query parameter");
		assertEquals(64, imageData.height, "Height should be 64 based on query parameter");
	}

	@Test
	public void testQueryParameterWithZoom200() throws Exception {
		URL baseUrl = URLHintProviderTest.class.getResource("/icons/imagetests/test-icon.svg");
		assertNotNull(baseUrl, "Test SVG not found");

		String urlString = baseUrl.toExternalForm();
		URL url = new URL(urlString + "?size=16x16");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(200);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(32, imageData.width, "Width should be 32 (16*2) at 200% zoom");
		assertEquals(32, imageData.height, "Height should be 32 (16*2) at 200% zoom");
	}

	@Test
	public void testQueryParameterRectangularSize() throws Exception {
		URL baseUrl = URLHintProviderTest.class.getResource("/icons/imagetests/test-icon.svg");
		assertNotNull(baseUrl, "Test SVG not found");

		String urlString = baseUrl.toExternalForm();
		URL url = new URL(urlString + "?size=48x32");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(100);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(48, imageData.width, "Width should be 48 based on query parameter");
		assertEquals(32, imageData.height, "Height should be 32 based on query parameter");
	}

	@Test
	public void testNoHintDefaultSize() throws Exception {
		URL url = URLHintProviderTest.class.getResource("/icons/imagetests/test-icon.svg");
		assertNotNull(url, "Test SVG not found");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(100);

		assertNotNull(imageData, "ImageData should not be null");
		// Without hint, SVG should render at its native viewBox size (128x128)
		assertEquals(128, imageData.width, "Width should be 128 (native SVG size)");
		assertEquals(128, imageData.height, "Height should be 128 (native SVG size)");
	}

	@Test
	public void testQueryParameterPrecedenceOverPath() throws Exception {
		// Query parameter should take precedence over path hint
		URL baseUrl = URLHintProviderTest.class.getResource("/icons/imagetests/16x16/test-icon.svg");
		assertNotNull(baseUrl, "Test SVG not found");

		String urlString = baseUrl.toExternalForm();
		URL url = new URL(urlString + "?size=64x64");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(100);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(64, imageData.width, "Width should be 64 from query parameter, not 16 from path");
		assertEquals(64, imageData.height, "Height should be 64 from query parameter, not 16 from path");
	}

	@Test
	public void testQueryParameterWithMultipleParams() throws Exception {
		URL baseUrl = URLHintProviderTest.class.getResource("/icons/imagetests/test-icon.svg");
		assertNotNull(baseUrl, "Test SVG not found");

		String urlString = baseUrl.toExternalForm();
		// Test with multiple query parameters
		URL url = new URL(urlString + "?foo=bar&size=24x24&other=value");

		ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
		ImageData imageData = descriptor.getImageData(100);

		assertNotNull(imageData, "ImageData should not be null");
		assertEquals(24, imageData.width, "Width should be 24 from query parameter");
		assertEquals(24, imageData.height, "Height should be 24 from query parameter");
	}

}
