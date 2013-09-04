package hudson.plugins.git.browser;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitChangeSet.Path;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Git Browser for GitLab
 */
public class GitList extends GitRepositoryBrowser {

    private static final long serialVersionUID = 1L;
    private final URL url;

    @DataBoundConstructor
    public GitList(String url) throws MalformedURLException {
        this.url = normalizeToEndWithSlash(new URL(url));
    }

    public URL getUrl() {
        return url;
    }

    /**
     * Creates a link to the changeset
     *
     * http://[GitList URL]/commit/a9182a07750c9a0dfd89a8461adf72ef5ef0885b
     *
     * @param path
     * @return diff link
     * @throws IOException
     */
    @Override
    public URL getChangeSetLink(GitChangeSet changeSet) throws IOException {
        return new URL(url, url.getPath()+"commit/" + changeSet.getId().toString());
    }

    /**
     * Creates a link to the file diff.
     * http://[GitList URL]/commit/573670a3bb1f3b939e87f1dee3e99b6bfe281fcb#N
     *
     * @param path affected file path
     * @return diff link
     * @throws IOException
     */
    @Override
    public URL getDiffLink(Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT || path.getSrc() == null || path.getDst() == null
                || path.getChangeSet().getParentCommit() == null) {
            return null;
        }
        return getDiffLinkRegardlessOfEditType(path);
    }

    /**
     * Return a diff link regardless of the edit type by appending the index of the pathname in the changeset.
     *
     * @param path
     * @return
     * @throws IOException
     */
    private URL getDiffLinkRegardlessOfEditType(Path path) throws IOException {
        final GitChangeSet changeSet = path.getChangeSet();
        final ArrayList<String> affectedPaths = new ArrayList<String>(changeSet.getAffectedPaths());
        // Github seems to sort the output alphabetically by the path.
        Collections.sort(affectedPaths);
        final String pathAsString = path.getPath();
        final int i = Collections.binarySearch(affectedPaths, pathAsString);
        assert i >= 0;
        return new URL(getChangeSetLink(changeSet), "#" + String.valueOf(i));
    }

    /**
     * Creates a link to the file.
     * http://[GitList URL]/blob/573670a3bb1f3b939e87f1dee3e99b6bfe281fcb/src/main/java/hudson/plugins/git/browser/GitList.java
     *  Gitlist seems to have no URL for deleted files, so just return
     * a difflink instead.
     *
     * @param path file
     * @return file link
     * @throws IOException
     */
    @Override
    public URL getFileLink(Path path) throws IOException {
        if (path.getEditType().equals(EditType.DELETE)) {
            return getDiffLinkRegardlessOfEditType(path);
        } else {
            final String spec = "blob/" + path.getChangeSet().getId() + "/" + path.getPath();
            return new URL(url, url.getPath() + spec);
        }
    }


    @Extension
    public static class GitlistDescriptor extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "gitlist";
        }

        @Override
        public GitList newInstance(StaplerRequest req, JSONObject jsonObject) throws FormException {
            return req.bindParameters(GitList.class, "Gitlist.");
        }
    }

}
