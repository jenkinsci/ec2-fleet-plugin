package com.amazon.jenkins.ec2fleet;

import org.jvnet.hudson.test.JenkinsRule;

/**
 * Current min version Jenkins doesn't close log files correctly for Windows, this class fix problem
 * by forcing log delete, so Jenkins inside test could correctly delete slave data to stop it
 * <p>
 * Special fix https://wiki.jenkins.io/display/JENKINS/Unit+Test+on+Windows#UnitTestonWindows-Unabletodeleteslave-slaveX.log
 * <code>
 * [2020-01-29T21:59:39.067Z] [ERROR] com.amazon.jenkins.ec2fleet.ProvisionIntegrationTest.should_continue_update_after_termination
 * Time elapsed: 255.077 s  <<< ERROR!
 * [2020-01-29T21:59:39.067Z] java.io.IOException: Unable to delete
 * 'C:\Jenkins\workspace\eet-plugin_configuration-as-code\target\tmp\j h6486687973724081781\logs\slaves\i-0\slave.log'.
 * Tried 3 times (of a maximum of 3) waiting 0.1 sec between attempts.
 * </code>
 */
class JenkinsRuleWithForcedLogPurge extends JenkinsRule {

//    private void purgeSlaves() {
//        final List<Computer> disconnectingComputers = new ArrayList<>();
//        final List<VirtualChannel> closingChannels = new ArrayList<>();
//        for (final Computer computer : jenkins.getComputers()) {
//            if (!(computer instanceof SlaveComputer)) {
//                continue;
//            }
//            // disconnect slaves.
//            // retrieve the channel before disconnecting.
//            // even a computer gets offline, channel delays to close.
//            if (!computer.isOffline()) {
//                final VirtualChannel ch = computer.getChannel();
//                computer.disconnect(null);
//                disconnectingComputers.add(computer);
//                closingChannels.add(ch);
//            }
//        }
//
//        try {
//            // Wait for all computers disconnected and all channels closed.
//            for (Computer computer : disconnectingComputers) {
//                computer.waitUntilOffline();
//            }
//            for (VirtualChannel ch : closingChannels) {
//                ch.join();
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void after() throws Exception {
//        if (Functions.isWindows()) {
////            purgeSlaves();
//        }
        super.after();
    }
}
