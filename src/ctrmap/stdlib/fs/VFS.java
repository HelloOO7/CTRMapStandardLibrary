package ctrmap.stdlib.fs;

import ctrmap.stdlib.fs.accessors.arc.DotArc;
import ctrmap.stdlib.fs.accessors.arc.ArcInput;
import ctrmap.stdlib.fs.accessors.arc.ArcFile;
import ctrmap.stdlib.fs.accessors.arc.ArcFileAccessor;
import ctrmap.stdlib.util.ProgressMonitor;
import java.util.ArrayList;
import java.util.List;

/**
 * A layered file system with support for archive mounting.
 */
public class VFS {

	private final FSManager fs;

	private VFSRootFile root;
	private VFSRootFile overlay;

	private boolean hasChangeBlacklist = false;
	private VFSChangeBlacklist blacklist;

	/**
	 * Creates a VFS using the provided FSManager.
	 *
	 * @param fs
	 */
	public VFS(FSManager fs) {
		this.fs = fs;
	}
	
	public VFSChangeBlacklist getChangeBlacklist(){
		return blacklist;
	}

	/**
	 * Initializes the VFS with the given layer roots.
	 *
	 * @param root Root file of the base layer.
	 * @param location Root file of the overlay.
	 */
	public void initVFS(VFSRootFile root, VFSRootFile location) {
		this.root = root;
		overlay = location;

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (hasChangeBlacklist) {
					blacklist.doRemoveFiles();
					blacklist.terminate();
				}
			}
		});
	}

	/**
	 * Gets the FSManager associated with this VFS.
	 *
	 * @return
	 */
	public FSManager getFS() {
		return fs;
	}

	/**
	 * Creates a file change blacklist at the given location and assigns it to
	 * this VFS.
	 *
	 * @param location A FSFile to write the change info into.
	 */
	public void createChangeBlacklist(FSFile location) {
		blacklist = new VFSChangeBlacklist(location, this);
		hasChangeBlacklist = true;
	}

	/**
	 * Gets the root file of the base layer.
	 *
	 * @return
	 */
	public FSFile getBaseFSRoot() {
		return root;
	}

	/**
	 * Gets the root file of the overlay layer.
	 *
	 * @return
	 */
	public FSFile getOvFSRoot() {
		return overlay;
	}

	/**
	 * Applies all contents of the OvFS, starting at 'path'.
	 *
	 * @param path The path to apply.
	 */
	public void applyOvFS(String path) {
		applyOvFS(path, null);
	}

	/**
	 * Applies all contents of the OvFS, starting at 'path',
	 *
	 * @param path The path to apply.
	 * @param monitor A progress monitor interface.
	 */
	public void applyOvFS(String path, ProgressMonitor monitor) {
		path = fs.getWildCardManager().getWildCardedPath(getRelativePath(path));
		FSFile ovFile = fs.getFileFromRefPath(overlay, path);
		FSFile target = fs.getFileFromRefPath(root, path);

		if (target.exists()) {
			if (ovFile.isDirectory()) {
				if (monitor != null) {
					monitor.setProgressTitle("Packing directory " + path);
					monitor.setProgressPercentage(0);
				}
				if (target instanceof ArcFile) {
					System.out.println("Applying arcfile..." + target.getPath());
					ArcFile arc = (ArcFile) target;
					if (monitor != null) {
						monitor.setProgressPercentage(0);
						monitor.setProgressSubTitle("Patching ArcFile...");
					}
					applyToArcFile(ovFile, ovFile, arc, monitor);
				} else {
					for (FSFile sub : ovFile.listFiles()) {
						applyOvFS(sub.getPath(), monitor);
					}
				}
			} else {
				if (target.isFile()) {
					if (monitor != null) {
						monitor.setProgressSubTitle("Writing " + target.getName());
						List<String> siblings = target.getParent().list();
						monitor.setProgressPercentage((int) (siblings.indexOf(target.getPath()) / (float) siblings.size() * 100));
					}
					if (!isFileChangeBlacklisted(path)) {
						System.out.println("Write " + path + " to " + target.getPath());
						FSUtil.writeBytesToFile(target, FSUtil.readFileToBytes(ovFile));
					}
				}
			}
		} else {
			if (ovFile instanceof ArcFile) {
				ovFile = ((ArcFile) ovFile).getSource(); //If the ArcFile was kept, it would get extracted to the target because ArcFile is a directory
			}

			FSUtil.copy(ovFile, target);
		}
	}

	private void applyToArcFile(FSFile root, FSFile fsf, ArcFile arc, ProgressMonitor monitor) {
		List<ArcInput> inputs = getArcInputs(root, fsf);
		ensureDotArcExistence(inputs, root);
		fs.getArcFileAccessor().writeToArcFile(arc, monitor, inputs.toArray(new ArcInput[inputs.size()]));
	}

	private static void ensureDotArcExistence(List<ArcInput> inputs, FSFile repackRoot) {
		for (ArcInput in : inputs) {
			if (in.targetPath.equals(DotArc.DOT_ARC_SIGNATURE)) {
				return;
			}
		}
		ArcInput dotArc = new ArcInput(DotArc.DOT_ARC_SIGNATURE, repackRoot.getChild(DotArc.DOT_ARC_SIGNATURE));
		inputs.add(dotArc);
	}

	private List<ArcInput> getArcInputs(FSFile root, FSFile fsf) {
		List<ArcInput> inputs = new ArrayList<>();
		if (fsf.isDirectory()) {
			for (FSFile sub : fsf.listFiles()) {
				inputs.addAll(getArcInputs(root, sub));
			}
		} else {
			ArcInput thisInput = new ArcInput(fsf.getPathRelativeTo(root), fsf);
			String fsPath = fs.getWildCardManager().getWildCardedPath(getRelativePath(fsf.getPath()));
			if (!isFileChangeBlacklisted(fsPath)) {
				System.out.println("Include ArcInput " + fsPath);
				inputs.add(thisInput);
			}
		}
		return inputs;
	}

	/**
	 * Notifies the VFS to remove a file from the blacklist as a result of its
	 * modification. The method does nothing if no blacklist is assigned.
	 *
	 * @param changedPath Wildcarded path of the changed file.
	 */
	public void notifyFileChange(String changedPath) {
		if (hasChangeBlacklist) {
			blacklist.removePathFromBlacklist(changedPath);
		}
	}

	/**
	 * Notifies the VFS that a new file has been created in the OvFS. This will
	 * register the file in the blacklist, if one is present.
	 *
	 * @param path Wildcarded path of the added file.
	 */
	public void notifyOvFsNewFileInit(String path) {
		if (hasChangeBlacklist) {
			blacklist.putBlacklistPath(path);
		}
	}

	/**
	 * Renames a path in the blacklist to another. Does nothing if no blacklist
	 * is attached.
	 *
	 * @param oldPath The path to be renamed.
	 * @param newPath Name of the path after renaming.
	 */
	public void relocateBlackListFile(String oldPath, String newPath) {
		if (hasChangeBlacklist) {
			blacklist.relocatePaths(oldPath, newPath);
		}
	}

	/**
	 * Checks if a file path is in the blacklist.
	 *
	 * @param path Wildcarded path of the file.
	 * @return Whether or not the blacklist contains the path, or false if no
	 * blacklist is attached.
	 */
	public boolean isFileChangeBlacklisted(String path) {
		if (hasChangeBlacklist) {
			return blacklist.hasPath(path);
		}
		return false;
	}

	/**
	 * Gets a file from either the OvFS or BaseFS using a path, accounting for
	 * ArcFile mounting and expansion.
	 *
	 * @param path Wildcarded path of the requested file.
	 * @return A VFSFile descriptor linked to the OvFS and BaseFS results.
	 */
	public FSFile getFile(String path) {
		//System.out.println("Requested file " + path);
		ArcFileAccessor afa = fs.getArcFileAccessor();
		path = getRelativePath(path);
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		boolean isExistingBase = false;
		FSFile existing = fs.getFileFromRefPath(overlay, path);
		if (existing != null && !existing.exists()) {
			isExistingBase = true;
			existing = fs.getFileFromRefPath(root, path);
		}
		FSFile result;
		if (existing != null && existing.exists()) {
			if (isExistingBase) {
				result = new VFSFile(path, this, existing);
			} else {
				result = new VFSFile(path, this);
			}
		} else {
			//If it does not exist, we will create empty files on the overlay, also expand ArcFiles with an accessor
			String[] splitPath = path.split("/");
			result = overlay;
			for (int t = 0; t < splitPath.length; t++) {
				String token = splitPath[t];
				if (token.startsWith(":") && token.endsWith(":")) {
					result = fs.getWildCardManager().getExistingRefFile(result, token);
				} else {
					result = result.getChild(token);
				}
				//If expandArcs is allowed, this takes into account ArcFiles in origin and casts them accordingly
				//The ArcFileAccessor will then deliver the extracted ArcFileMember with its implementation
				FSFile origin = root.getMatchingChild(result.getPathRelativeTo(overlay), fs.getWildCardManager());
				if (origin != null && afa.isArcFile(origin)) {
					//System.out.println("Expanding ArcFile " + origin.getPath());
					ArcFile af = new ArcFile(origin, afa);
					result = af;

					StringBuilder pathInArc = new StringBuilder();
					for (int t2 = t + 1; t2 < splitPath.length; t2++) {
						pathInArc.append("/");
						pathInArc.append(splitPath[t2]);
					}
					if (pathInArc.length() > 0) {
						result = new VFSFile(path, this, af.getChild(pathInArc.toString()));
						break;
					}
				}
			}
		}
		//System.out.println("Got file " + result + " for request " + path);
		return result;
	}

	/**
	 * Converts a path to be relative to either the BaseFS or OvFS.
	 *
	 * @param path The path to convert.
	 * @return The input path, relative.
	 */
	public String getRelativePath(String path) {
		path = FSFile.getPathRelativeTo(path, overlay.getPath());
		path = FSFile.getPathRelativeTo(path, root.getPath());
		return path;
	}
}
