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
package org.eclipse.jface.snippets.resources;

import java.net.URL;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A snippet to demonstrate SVG image size hints using path-based and
 * query-parameter-based size detection.
 * 
 * <p>
 * This demonstrates two ways to control SVG rendering size:
 * <ol>
 * <li><b>Path-based hints:</b> Place SVG in folders like /icons/16x16/ or
 * /icons/32x32/</li>
 * <li><b>Query parameter hints:</b> Add ?size=WIDTHxHEIGHT to the URL</li>
 * </ol>
 * </p>
 * 
 * <p>
 * This allows using a single SVG file at different sizes without creating
 * multiple scaled versions or restricting the SVG design size.
 * </p>
 */
public class Snippet083SVGImageSizeHints {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("SVG Image Size Hints Demo");
		GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).spacing(10, 10).applyTo(shell);

		addSection(shell, "Path-Based Size Hints", """
				SVGs placed in folders with size patterns (e.g., /icons/16x16/, /icons/32x32/)
				are automatically rendered at that size.

				Example: bundle://plugin.id/icons/16x16/icon.svg
				""");

		addSection(shell, "Query Parameter Size Hints", """
				You can specify size using query parameters for maximum flexibility.
				This allows using the same SVG at different sizes.

				Example: bundle://plugin.id/icons/icon.svg?size=16x16
				Example: bundle://plugin.id/icons/icon.svg?size=128x128
				""");

		addSection(shell, "Zoom Support", """
				Both methods work with high-DPI displays. The hint specifies the base size,
				and JFace automatically scales for 150% and 200% zoom levels.

				16x16 at 100% zoom → 16x16 pixels
				16x16 at 150% zoom → 24x24 pixels
				16x16 at 200% zoom → 32x32 pixels
				""");

		// Demonstrate with actual images from test resources if available
		try {
			URL svgUrl = Snippet083SVGImageSizeHints.class
					.getResource("/org/eclipse/jface/snippets/resources/test-demo.svg");
			if (svgUrl != null) {
				addImageDemo(shell, "Default (no hint)", svgUrl.toString(), display);
				addImageDemo(shell, "With ?size=32x32", svgUrl.toString() + "?size=32x32", display);
				addImageDemo(shell, "With ?size=64x64", svgUrl.toString() + "?size=64x64", display);
			}
		} catch (Exception e) {
			// Demo images not available, that's okay
			Label note = new Label(shell, SWT.WRAP);
			note.setText("Note: Visual demo requires test SVG files. See URLHintProviderTest for examples.");
			GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(note);
		}

		addSection(shell, "Use Cases", """
				1. Toolbar icons: Use ?size=16x16 for consistent toolbar sizing
				2. Wizard images: Use ?size=128x128 for large wizard graphics
				3. View icons: Place in /icons/16x16/ folder structure
				4. Multi-resolution: One SVG serves all sizes without duplication
				""");

		addSection(shell, "Code Example", """
				// Using query parameter
				URL iconUrl = new URL("bundle://my.plugin/icons/search.svg?size=16x16");
				ImageDescriptor desc = ImageDescriptor.createFromURL(iconUrl);
				Image icon = desc.createImage();

				// Using path-based hint
				URL iconUrl = FileLocator.find(bundle, new Path("icons/16x16/search.svg"));
				ImageDescriptor desc = ImageDescriptor.createFromURL(iconUrl);
				""");

		shell.pack();
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private static void addSection(Shell shell, String title, String text) {
		Label titleLabel = new Label(shell, SWT.BOLD);
		titleLabel.setText(title);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(titleLabel);

		Label textLabel = new Label(shell, SWT.WRAP);
		textLabel.setText(text.trim());
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).hint(600, SWT.DEFAULT).applyTo(textLabel);

		Label separator = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(separator);
	}

	private static void addImageDemo(Shell shell, String description, String urlString, Display display) {
		try {
			URL url = new URL(urlString);
			ImageDescriptor descriptor = ImageDescriptor.createFromURL(url);
			Image image = descriptor.createImage(display);

			Label label = new Label(shell, SWT.NONE);
			label.setText(description + ":");
			GridDataFactory.fillDefaults().applyTo(label);

			Canvas canvas = new Canvas(shell, SWT.BORDER);
			canvas.addPaintListener(e -> {
				if (!image.isDisposed()) {
					e.gc.drawImage(image, 0, 0);
				}
			});
			GridDataFactory.swtDefaults().hint(image.getBounds().width + 2, image.getBounds().height + 2)
					.applyTo(canvas);

			canvas.addDisposeListener(e -> {
				if (!image.isDisposed()) {
					image.dispose();
				}
			});
		} catch (Exception e) {
			Label error = new Label(shell, SWT.NONE);
			error.setText(description + ": Error loading image");
			GridDataFactory.fillDefaults().span(2, 1).applyTo(error);
		}
	}
}
