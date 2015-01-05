/*******************************************************************************
 * Copyright (c) 2013, 2014 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Initial API and implementation and/or initial documentation - Jay Jay Billings,
 *   Jordan H. Deyton, Dasha Gorin, Alexander J. McCaskey, Taylor Patterson,
 *   Claire Saunders, Matthew Wang, Anna Wojtowicz
 *******************************************************************************/
package org.eclipse.ice.caebat.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ice.datastructures.ICEObject.Component;
import org.eclipse.ice.datastructures.form.AllowedValueType;
import org.eclipse.ice.datastructures.form.DataComponent;
import org.eclipse.ice.datastructures.form.Entry;
import org.eclipse.ice.datastructures.form.Form;
import org.eclipse.ice.datastructures.form.FormStatus;
import org.eclipse.ice.datastructures.form.MasterDetailsComponent;
import org.eclipse.ice.datastructures.form.TableComponent;
import org.eclipse.ice.io.ips.IPSReader;
import org.eclipse.ice.io.ips.IPSWriter;
import org.eclipse.ice.item.Item;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * <!-- begin-UML-doc -->
 * <p>
 * This class is the model representation of the CAEBAT model. It inherits from
 * the Item Class. It will load INI conf files into a form that can be written to 
 * create new input for CAEBAT Simulations.  If no conf file is given to the 
 * loadInput method the CaebatModel will automatically load the case 6 example.
 * </p>
 * <!-- end-UML-doc -->
 * 
 * @author s4h, bzq
 */
@XmlRootElement(name = "CaebatModel")
public class CaebatModel extends Item {

	/**
	 * <!-- begin-UML-doc -->
	 * <p>
	 * A custom tag for ini files operation. Set in the constructor.
	 * </p>
	 * <!-- end-UML-doc -->
	 */
	private String customTaggedExportString = "Export to Caebat INI format";

	// The name of the example chosen
	protected String exampleName; // Default for now

	/**
	 * A nullary constructor that delegates to the project constructor.
	 */
	public CaebatModel() {
		this(null);
		return;
	}

	/**
	 * <!-- begin-UML-doc -->
	 * <p>
	 * The constructor for the CaebatModel. Calls the constructor for Item by
	 * passing the IProject. It should call setupForm() in the super
	 * constructor.
	 * </p>
	 * <!-- end-UML-doc -->
	 * 
	 * @param project
	 *            <p>
	 *            The passed IProject for the workspace.
	 *            </p>
	 */
	public CaebatModel(IProject project) {

		// begin-user-code

		// Setup the form and everything
		super(project);
		return;
		// end-user-code

	}

	/**
	 * <!-- begin-UML-doc -->
	 * <p>
	 * This operation overrides the Item.setupForm() operation.
	 * </p>
	 * <!-- end-UML-doc -->
	 */
	public void setupForm() {
		// begin-user-code

		// This method will create a new Form and add all the dataComponents to
		// the form. These dataComponents will be accessed later in
		// loadDataComponents.
		form = new Form();
		ArrayList<String> problemFiles = null;
		String separator = System.getProperty("file.separator");
		// Setup Item information
		setName("Caebat Model");
		setDescription("This model creates input for CAEBAT.");

		// If loading from the new item button we should just
		// load up the default case 6 file by passing in null
		loadInput(null);

		// Add an action to the list to allow for the INI exports
		customTaggedExportString = "Export to Caebat INI format";
		allowedActions.add(0, customTaggedExportString);

		// ----- Finish setting up the Form so that it can be immediately
		// launched

		return;
		// end-user-code

	}

	/**
	 * <!-- begin-UML-doc -->
	 * <p>
	 * Overrides the reviewEntries operation. This will still call
	 * super.reviewEntries, but will handle the dependencies after all other dep
	 * handing is finished.
	 * </p>
	 * <!-- end-UML-doc -->
	 * 
	 * @return the status of the form
	 */
	protected FormStatus reviewEntries(Form preparedForm) {

		// begin-user-code
		FormStatus retStatus = FormStatus.ReadyToProcess;
		Component dataComp = null;
		
		// Grab the data component from the Form and only proceed if it exists
		ArrayList<Component> components = preparedForm.getComponents();
		dataComp = components.get(0);

		// Make sure the form has some data
		if (dataComp == null || !"Time Loop Data".equals(dataComp.getName())) {
			retStatus = FormStatus.InfoError;
		}
		return retStatus;
	}

	/**
	 * <!-- begin-UML-doc -->
	 * <p>
	 * Overrides item's process by adding a customTaggedExportString (ini).
	 * Still utilizes Item's process functionality for all other calls.
	 * </p>
	 * <!-- end-UML-doc -->
	 */
	public FormStatus process(String actionName) {
		// begin-user-code
		FormStatus retStatus;

		// If it is the custom operation, call this here.
		if (this.customTaggedExportString.equals(actionName)) {

			// Get the file from the project space to create the output
			String filename = getName().replaceAll("\\s+", "_") + "_" + getId()
					+ ".conf";
			String filePath = project.getLocation().toOSString() + 
					System.getProperty("file.separator") + filename;
			
			// Get the file path and build the URI that will be used to write
			IFile outputFile = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(new Path(filePath));

			// Get the data from the form
			ArrayList<Component> components = form.getComponents();

			// A valid CaebatModel needs 4 components
			if (components.size() > 3) {

				// create a new IPSWriter with the output file
				IPSWriter writer = new IPSWriter();
				try {
					// Write the output file
					writer.write(form, outputFile);
					// Refresh the project space
					project.refreshLocal(IResource.DEPTH_ONE, null);
				} catch (CoreException e) {
					// Complain
					System.err.println("CaebatModel Message: "
							+ "Failed to refresh the project space.");
					e.printStackTrace();
				}
				// return a success
				retStatus = FormStatus.Processed;
			} else {
				// return an error
				System.err.println("Not enough components to write new file!");
				retStatus = FormStatus.InfoError;
			}
		}

		// Otherwise let item deal with the process
		else {
			retStatus = super.process(actionName);
		}

		return retStatus;
		// end-user-code

	}

	/**
	 * <!-- begin-UML-doc -->
	 * <p>
	 * This operation loads the given example into the Form.
	 * </p>
	 * <!-- end-UML-doc -->
	 * 
	 * @param name
	 *            The path name of the example file name to load.
	 */
	public void loadInput(String name) {

		// If nothing is specified, load case 6 from inside the plugin
		IFile inputFile = null;
		if (name == null) {
			try {
				// Path to the default file
				String defaultFilePath = null;
				if (project != null) {
					defaultFilePath = project.getLocation().toOSString() + 
							System.getProperty("file.separator") + "case_6.conf";
				} else {
					defaultFilePath = ResourcesPlugin.getWorkspace().getRoot().getLocation()
							.toOSString() + System.getProperty("file.separator") + "case_6.conf";
				}

				File temp = new File(defaultFilePath);
				if (!temp.exists()) {
					temp.createNewFile();
				}
				URI uri = new URI("platform:/plugin/org.eclipse.ice.caebat/data/case_6.conf");
				InputStream reader  = uri.toURL().openStream();
				FileOutputStream outStream = new FileOutputStream(temp);
				
				int fileByte;
				while ((fileByte = reader.read()) != -1) {
					outStream.write(fileByte);
				}
				inputFile = ResourcesPlugin.getWorkspace().getRoot()
						.getFile(new Path(defaultFilePath));
				
			} catch (URISyntaxException e) {
				System.err.println("CaebatModel Message: Error!  Could not load the default" 
						+ " Caebat case data!");
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			String filePath = null;
			if (project != null) {
				filePath = project.getLocation().toOSString() + 
						System.getProperty("file.separator") + name;
			} else {
				filePath = ResourcesPlugin.getWorkspace().getRoot().getLocation()
						.toOSString() + System.getProperty("file.separator") + name;
			}
			inputFile = ResourcesPlugin.getWorkspace().getRoot()
					.getFile(new Path(filePath));
		}
		
		// Load the components from the file
		IPSReader reader = new IPSReader();
		Form newForm = reader.read(inputFile);
		
		// Put the loaded information in the CaebatModel form
		// TODO : Should I just reassign?  .. form = reader.read(uri) ??
		form.copy(newForm);
	}
}
