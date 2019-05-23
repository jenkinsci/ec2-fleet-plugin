package com.amazon.jenkins.ec2fleet;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Reservation;

import java.util.HashSet;
import java.util.Set;

public class EC2Api {

    public static Set<String> describeTerminated(final AmazonEC2 ec2, final Set<String> instanceIds) {
        // assume all terminated until we get opposite info
        final Set<String> terminated = new HashSet<>(instanceIds);
        // don't do actual call if no data
        if (instanceIds.isEmpty()) return terminated;

        final DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceIds);

        final Set<String> terminatedStates = new HashSet<>();
        terminatedStates.add(InstanceStateName.Terminated.toString());
        terminatedStates.add(InstanceStateName.Stopped.toString());
        terminatedStates.add(InstanceStateName.Stopping.toString());
        terminatedStates.add(InstanceStateName.ShuttingDown.toString());

        DescribeInstancesResult result;
        do {
            result = ec2.describeInstances(request);
            request.setNextToken(result.getNextToken());

            for (final Reservation r : result.getReservations()) {
                for (final Instance instance : r.getInstances()) {
                    // if instance not in terminated state, remove it from terminated
                    if (!terminatedStates.contains(instance.getState().getName())) {
                        terminated.remove(instance.getInstanceId());
                    }
                }
            }
        } while (result.getNextToken() != null);

        return terminated;
    }

}
