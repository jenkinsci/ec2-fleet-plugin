package com.amazon.jenkins.ec2fleet.exceptions;

public class TerminateAutoScalingException extends RuntimeException{
    public TerminateAutoScalingException(String message) {
        super(message);
    }
}
