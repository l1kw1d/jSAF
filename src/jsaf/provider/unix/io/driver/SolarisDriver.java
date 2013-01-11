// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

package jsaf.provider.unix.io.driver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.slf4j.cal10n.LocLogger;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFileMetadata;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.io.IReader;
import jsaf.intf.unix.io.IUnixFileInfo;
import jsaf.intf.unix.io.IUnixFilesystem;
import jsaf.intf.unix.io.IUnixFilesystemDriver;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.intf.util.ISearchable;
import jsaf.io.PerishableReader;
import jsaf.provider.unix.io.UnixFileInfo;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

/**
 * IUnixFilesystemDriver implementation for Solaris.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SolarisDriver extends AbstractDriver {
    public SolarisDriver(IUnixSession session) {
	super(session);
    }

    // Implement IUnixFilesystemDriver

    public Collection<IFilesystem.IMount> getMounts(Pattern typeFilter) throws Exception {
	Collection<IFilesystem.IMount> mounts = new ArrayList<IFilesystem.IMount>();
	IFile f = session.getFilesystem().getFile("/etc/vfstab");
	IReader reader = PerishableReader.newInstance(f.getInputStream(), session.getTimeout(IUnixSession.Timeout.S));
	String line = null;
	while ((line = reader.readLine()) != null) {
	    if (!line.startsWith("#")) { // skip comments
		StringTokenizer tok = new StringTokenizer(line);
		String dev = tok.nextToken();
		String fixdev = tok.nextToken();
		String mountPoint = tok.nextToken();
		String fsType = tok.nextToken();
		if (typeFilter != null && typeFilter.matcher(fsType).find()) {
		    logger.info(Message.STATUS_FS_MOUNT_SKIP, mountPoint, fsType);
		} else if (mountPoint.startsWith(IUnixFilesystem.DELIM_STR)) {
		    logger.info(Message.STATUS_FS_MOUNT_ADD, mountPoint, fsType);
		    mounts.add(new Mount(mountPoint, fsType));
		}
	    }
	}
	return mounts;
    }

    public String getFindCommand(List<ISearchable.ICondition> conditions) {
	String from = null;
	boolean dirOnly = false;
	boolean followLinks = false;
	boolean xdev = false;
	Pattern path = null, dirname = null, basename = null;
	String literalBasename = null, antiBasename = null;
	int depth = 1;

	for (ISearchable.ICondition condition : conditions) {
	    switch(condition.getField()) {
	      case IUnixFilesystem.FIELD_FOLLOW_LINKS:
		followLinks = true;
		break;
	      case IUnixFilesystem.FIELD_XDEV:
		xdev = true;
		break;
	      case IFilesystem.FIELD_FILETYPE:
		if (IFilesystem.FILETYPE_DIR.equals(condition.getValue())) {
		    dirOnly = true;
		}
		break;
	      case IFilesystem.FIELD_PATH:
		path = (Pattern)condition.getValue();
		break;
	      case IFilesystem.FIELD_DIRNAME:
		dirname = (Pattern)condition.getValue();
		break;
	      case IFilesystem.FIELD_BASENAME:
		switch(condition.getType()) {
		  case ISearchable.TYPE_EQUALITY:
		    literalBasename = (String)condition.getValue();
		    break;
		  case ISearchable.TYPE_INEQUALITY:
		    antiBasename = (String)condition.getValue();
		    break;
		  case ISearchable.TYPE_PATTERN:
		    basename = (Pattern)condition.getValue();
		    break;
		}
		break;
	      case ISearchable.FIELD_DEPTH:
		depth = ((Integer)condition.getValue()).intValue();
		break;
	      case ISearchable.FIELD_FROM:
		from = ((String)condition.getValue()).replace(" ", "\\ ");
		break;
	    }
	}

	StringBuffer sb = new StringBuffer("find");
	if (followLinks) {
	    sb.append(" -L");
	}
	String FIND = sb.toString();
	StringBuffer cmd = new StringBuffer(FIND).append(" ").append(from);
	if (xdev) {
	    cmd.append(" -mount");
	}
	if (path == null) {
	    if (dirname == null) {
		if (dirOnly && depth != ISearchable.DEPTH_UNLIMITED) {
		    cmd.append(" -type d");
		    cmd.append(" -print");
		    if (depth == 1 && "/".equals(from)) {
			cmd.append(" ! -name /");
		    }
		    cmd.append(" -exec sh -c 'echo $1 | awk -F/ '\\''{if (NF > ");
		    cmd.append(Integer.toString(getDepth(from) + depth));
		    cmd.append(") {exit 0} else {exit 1}}'\\''' {} {} \\; -prune");
		} else if (!dirOnly) {
		    cmd.append(" -type f");
		    if (literalBasename != null) {
			cmd.append(" -name '").append(literalBasename).append("'");
		    } else if (antiBasename != null) {
			cmd.append(" ! -name '").append(antiBasename).append("'");
		    }
		    cmd.append(" -print");
		    if (depth != ISearchable.DEPTH_UNLIMITED) {
			cmd.append(" -o -type d");
			if (depth == 1 && "/".equals(from)) {
			    cmd.append(" ! -name /");
			}
			cmd.append(" -exec sh -c 'echo $1 | awk -F/ '\\''{if (NF > ");
			cmd.append(Integer.toString(getDepth(from) + depth));
			cmd.append(") {exit 0} else {exit 1}}'\\''' {} {} \\; -prune");
		    }
		    if (basename != null) {
			cmd.append(" | /usr/xpg4/bin/awk -F/ '$NF ~ /").append(basename.pattern()).append("/'");
		    }
		}
	    } else {
		cmd.append(" -type d");
		cmd.append(" | /usr/xpg4/bin/grep -E '").append(dirname.pattern()).append("'");
		if (!dirOnly) {
		    cmd.append(" | xargs -I[] ").append(FIND).append(" '[]' -type f");
		    if (antiBasename != null) {
			cmd.append(" ! -name '").append(antiBasename).append("'");
		    } else if (literalBasename != null) {
			cmd.append(" -name '").append(literalBasename).append("'");
		    }
		    if (depth != ISearchable.DEPTH_UNLIMITED) {
			// It's impossible for a filename to ever contain the \0 character, so that's our token
			cmd.append(" -exec echo []\\0 {} \\; | awk -F\\0 'split($1,a,\"/\")+");
			cmd.append(Integer.toString(depth));
			cmd.append(" >= split($2,b,\"/\"){print substr($2,2)}'");
		    }
		    if (basename != null) {
			cmd.append(" | /usr/xpg4/bin/awk -F/ '$NF ~ /").append(basename.pattern()).append("/'");
		    }
		}
	    }
	} else {
	    cmd.append(" -type f");
	    cmd.append(" | /usr/xpg4/bin/grep -E '").append(path.pattern()).append("'");
	}
	cmd.append(" | xargs -i ").append(getStatCommand()).append(" '{}'");
	return cmd.toString();
    }

    public String getStatCommand() {
	return "ls -dnE";
    }

    public UnixFileInfo nextFileInfo(Iterator<String> lines) {
	String line = null;
	if (lines.hasNext()) {
	    line = lines.next();
	} else {
	    return null;
	}

	char unixType = line.charAt(0);
	String permissions = line.substring(1, 10);
	boolean hasExtendedAcl = false;
	if (line.charAt(10) == '+') {
	    hasExtendedAcl = true;
	}

	StringTokenizer tok = new StringTokenizer(line.substring(11));
	String linkCount = tok.nextToken();
	int uid = -1;
	try {
	    uid = Integer.parseInt(tok.nextToken());
	} catch (NumberFormatException e) {
	    //DAS -- could be, e.g., 4294967294 (illegal "nobody" value)
	}
	int gid = -1;
	try {
	    gid = Integer.parseInt(tok.nextToken());
	} catch (NumberFormatException e) {
	    //DAS -- could be, e.g., 4294967294 (illegal "nobody" value)
	}

	IFileMetadata.Type type = IFileMetadata.Type.FILE;
	switch(unixType) {
	  case IUnixFileInfo.DIR_TYPE:
	    type = IFileMetadata.Type.DIRECTORY;
	    break;

	  case IUnixFileInfo.LINK_TYPE:
	    type = IFileMetadata.Type.LINK;
	    break;

	  case IUnixFileInfo.CHAR_TYPE:
	  case IUnixFileInfo.BLOCK_TYPE:
	    int ptr = -1;
	    if ((ptr = line.indexOf(",")) > 11) {
		tok = new StringTokenizer(line.substring(ptr+1));
	    }
	    break;
	}

	long length = 0;
	try {
	    length = Long.parseLong(tok.nextToken());
	} catch (NumberFormatException e) {
	}

	long mtime = IFile.UNKNOWN_TIME;
	String dateStr = tok.nextToken("/").trim();
	try {
	    String parsable = new StringBuffer(dateStr.substring(0, 23)).append(dateStr.substring(29)).toString();
	    mtime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").parse(parsable).getTime();
	} catch (ParseException e) {
	    e.printStackTrace();
	}

	String path = null, linkPath = null;
	int begin = line.indexOf(session.getFilesystem().getDelimiter());
	if (begin > 0) {
	    int end = line.indexOf("->");
	    if (end == -1) {
		path = line.substring(begin).trim();
	    } else if (end > begin) {
		path = line.substring(begin, end).trim();
		linkPath = line.substring(end+2).trim();
	    }
	}

	if (type == IFileMetadata.Type.LINK && linkPath == null) {
	    logger.warn(Message.ERROR_LINK_NOWHERE, path);
	    return nextFileInfo(lines);
	} else {
	    return new UnixFileInfo(type, path, linkPath, IFile.UNKNOWN_TIME, mtime, IFile.UNKNOWN_TIME, length,
				    unixType, permissions, uid, gid, hasExtendedAcl, null);
	}
    }
}
