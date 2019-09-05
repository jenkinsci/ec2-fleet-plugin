package com.amazon.jenkins.ec2fleet;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import hudson.util.LogTaskListener;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EC2FleetNode extends Slave implements EphemeralNode, EC2FleetCloudAware {

    private static final Logger LOGGER = Logger.getLogger(EC2FleetNode.class.getName());

    private volatile EC2FleetCloud cloud;

    @SuppressWarnings("WeakerAccess")
    public EC2FleetNode(final String name, final String nodeDescription, final String remoteFS, final int numExecutors, final Mode mode, final String label,
                        final List<? extends NodeProperty<?>> nodeProperties, final EC2FleetCloud cloud, ComputerLauncher launcher) throws IOException, Descriptor.FormException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, label,
                launcher, RetentionStrategy.NOOP, nodeProperties);
        this.cloud = cloud;
    }

    @Override
    public Node asNode() {
        return this;
    }

    @Override
    public String getDisplayName() {
        // in some multi-thread edge cases cloud could be null for some time, just be ok with that
        return (cloud == null ? "unknown fleet" : cloud.getDisplayName()) + " " + name;
    }

    @Override
    public Computer createComputer() {
        return new EC2FleetNodeComputer(this, name, cloud);
    }

    @Override
    public EC2FleetCloud getCloud() {
        return cloud;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCloud(@Nonnull EC2FleetCloud cloud) {
        this.cloud = cloud;
    }

    /**
     * Execute provided script on node machine. Script will be stored as temporary file
     * for execution. Script output/error will be reported as logs.
     *
     * @param script script to execute on a node
     * @return exit code from script process
     * @throws IOException
     * @throws InterruptedException
     */
    public int executeScript(final String script) throws IOException, InterruptedException {
        LOGGER.info(getNodeName() + " script to execute:");
        LOGGER.info(script);

        LOGGER.info(getNodeName() + " upload script");
        final FilePath rootFilePath = getRootPath();
        // todo merge create file, copy and chmod operation in one call if possible
        final FilePath tempFile = rootFilePath.createTempFile("temp", null);
        tempFile.copyFrom(new ByteArrayInputStream(script.getBytes()));
        @SuppressWarnings("OctalInteger") final int onlyUserExecutePermissions = 0500;
        tempFile.chmod(onlyUserExecutePermissions);

        LOGGER.info(getNodeName() + " executing script");
        final TaskListener taskListener = new LogTaskListener(LOGGER, Level.INFO);
        final Launcher launcher = rootFilePath.createLauncher(taskListener);
        final Launcher.ProcStarter procStarter = launcher.launch().cmds(tempFile.getRemote());
        procStarter.readStdout();

        final Proc proc = procStarter.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getStdout()))) {
                    while (true) {
                        final String line = reader.readLine();
                        if (line == null) return;
                        LOGGER.log(Level.INFO, getNodeName() + " script output >> " + line);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        final int exitCode = proc.joinWithTimeout(60, TimeUnit.SECONDS, taskListener);
        LOGGER.log(Level.INFO, getNodeName() + " script exit code " + exitCode);

        return exitCode;
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public DescriptorImpl() {
            super();
        }

        public String getDisplayName() {
            return "Fleet Slave";
        }

        /**
         * We only create this kind of nodes programmatically.
         */
        @Override
        public boolean isInstantiable() {
            return false;
        }
    }

}
