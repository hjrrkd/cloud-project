package org.example;

import java.util.Scanner;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.jcraft.jsch.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;



public class Main {

    static AmazonEC2 ec2;

    private static void init() throws Exception {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (C:\\Users\\heeju\\.aws\\credentials), and is in valid format.",
                    e);
        }

        // 리전을 ap-southeast-2로 고정
        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion("ap-northeast-2")
                .build();
    }

    public static void main(String[] args) throws Exception {
        init();

        Scanner menu = new Scanner(System.in);
        Scanner idString = new Scanner(System.in);
        int number = 0;

        while (true) {
            System.out.println("\n------------------------------------------------------------");
            System.out.println("           Amazon AWS Control Panel using SDK               ");
            System.out.println("------------------------------------------------------------");
            System.out.println("  1. list instance                2. available zones        ");
            System.out.println("  3. start instance               4. available regions      ");
            System.out.println("  5. stop instance                6. create instance        ");
            System.out.println("  7. reboot instance              8. list images            ");
            System.out.println("  9. execute condor_status       10. AutoScaling            ");
            System.out.println("                                  99. quit                   ");
            System.out.println("------------------------------------------------------------");

            System.out.print("Enter an integer: ");
            if (menu.hasNextInt()) {
                number = menu.nextInt();
            } else {
                System.out.println("Invalid input!");
                break;
            }

            String instanceId = "";
            String amiId = "";

            switch (number) {
                case 1:
                    listInstances();
                    break;
                case 2:
                    availableZones();
                    break;
                case 3:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) startInstance(instanceId);
                    break;
                case 4:
                    availableRegions();
                    break;
                case 5:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) stopInstance(instanceId);
                    break;
                case 6:
                    System.out.print("Enter ami id: ");
                    amiId = idString.nextLine().trim();
                    if (!amiId.isEmpty()) createInstance(amiId);
                    break;
                case 7:
                    System.out.print("Enter instance id: ");
                    instanceId = idString.nextLine().trim();
                    if (!instanceId.isEmpty()) rebootInstance(instanceId);
                    break;
                case 8:
                    listImages();
                    break;
                case 9:
                    executeCondorStatus();
                    break;
                case 10:
                    autoScaling();
                    break;
                case 99:
                    System.out.println("Exiting...");
                    menu.close();
                    idString.close();
                    return;

                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    public static void listInstances() {
        System.out.println("Listing instances...");
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        boolean done = false;

        while (!done) {
            DescribeInstancesResult response = ec2.describeInstances(request);

            for (Reservation reservation : response.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    System.out.printf(
                            "[id] %s, [AMI] %s, [type] %s, [state] %10s, [monitoring state] %s\n",
                            instance.getInstanceId(),
                            instance.getImageId(),
                            instance.getInstanceType(),
                            instance.getState().getName(),
                            instance.getMonitoring().getState());
                }
            }

            request.setNextToken(response.getNextToken());

            if (response.getNextToken() == null) {
                done = true;
            }
        }
    }

    public static void availableZones() {
        System.out.println("Available zones...");
        DescribeAvailabilityZonesResult result = ec2.describeAvailabilityZones();

        for (AvailabilityZone zone : result.getAvailabilityZones()) {
            System.out.printf("[Zone] %s, [Region] %s, [Zone Name] %s\n",
                    zone.getZoneId(), zone.getRegionName(), zone.getZoneName());
        }
    }

    public static void startInstance(String instanceId) {
        System.out.printf("Starting instance: %s\n", instanceId);

        try {
            StartInstancesRequest request = new StartInstancesRequest()
                    .withInstanceIds(instanceId);
            ec2.startInstances(request);
            System.out.printf("Successfully started instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to start instance: %s\n", e.getMessage());
        }
    }

    public static void stopInstance(String instanceId) {
        System.out.printf("Stopping instance: %s\n", instanceId);

        try {
            StopInstancesRequest request = new StopInstancesRequest()
                    .withInstanceIds(instanceId);
            ec2.stopInstances(request);
            System.out.printf("Successfully stopped instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to stop instance: %s\n", e.getMessage());
        }
    }

    public static void createInstance(String amiId) {
        System.out.printf("Creating instance in region: ap-southeast-2 with AMI: %s\n", amiId);

        try {
            RunInstancesRequest request = new RunInstancesRequest()
                    .withImageId(amiId)
                    .withInstanceType(InstanceType.T2Micro)
                    .withMinCount(1)
                    .withMaxCount(1);
            RunInstancesResult response = ec2.runInstances(request);
            String instanceId = response.getReservation().getInstances().get(0).getInstanceId();
            System.out.printf("Successfully created instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to create instance: %s\n", e.getMessage());
        }
    }

    public static void rebootInstance(String instanceId) {
        System.out.printf("Rebooting instance: %s\n", instanceId);

        try {
            RebootInstancesRequest request = new RebootInstancesRequest()
                    .withInstanceIds(instanceId);
            ec2.rebootInstances(request);
            System.out.printf("Successfully rebooted instance: %s\n", instanceId);
        } catch (Exception e) {
            System.out.printf("Failed to reboot instance: %s\n", e.getMessage());
        }
    }

    public static void availableRegions() {
        System.out.println("Available regions...");
        DescribeRegionsResult result = ec2.describeRegions();

        for (Region region : result.getRegions()) {
            System.out.printf("[Region] %s, [Endpoint] %s\n",
                    region.getRegionName(), region.getEndpoint());
        }
    }

    public static void listImages() {
        System.out.println("Listing images....");

        DescribeImagesRequest request = new DescribeImagesRequest();
        request.withFilters(new Filter().withName("name").withValues("aws-htcondor-worker"));

        try {
            DescribeImagesResult results = ec2.describeImages(request);

            for (Image image : results.getImages()) {
                System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
                        image.getImageId(), image.getName(), image.getOwnerId());
            }
        } catch (Exception e) {
            System.out.printf("Failed to list images: %s\n", e.getMessage());
        }
    }

    public static void executeCondorStatus() {
        // EC2 인스턴스 정보 설정
        String host = "ec2-3-38-98-219.ap-northeast-2.compute.amazonaws.com";
        String user = "ec2-user";
        String privateKeyPath = "C:\\Users\\heeju\\heeju-key.pem";


        try {
            // JSch 객체 생성
            JSch jsch = new JSch();
            jsch.addIdentity(privateKeyPath);  // 개인 키 파일 경로 추가

            // SSH 세션 생성
            Session session = jsch.getSession(user, host, 22);
            session.setConfig("StrictHostKeyChecking", "no");  // 호스트 키 확인 비활성화
            session.connect();  // 연결

            // SSH 채널 생성 (exec 채널을 통해 명령어 실행)
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("condor_status");  // 실행할 명령어 설정
            channel.setErrStream(System.err);  // 오류 출력 스트림 설정

            // 명령어 실행 후 결과를 읽기 위한 InputStream
            InputStream in = channel.getInputStream();
            channel.connect();  // 명령어 실행

            // 결과 출력
            byte[] buffer = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0)
                        break;
                    System.out.print(new String(buffer, 0, i));  // 명령어 출력 결과 출력
                }
                if (channel.isClosed()) {
                    if (in.available() > 0)
                        continue;
                    System.out.println("Exit status: " + channel.getExitStatus());  // 종료 상태 출력
                    break;
                }
                Thread.sleep(1000);  // 1초 대기
            }

            // 채널 및 세션 종료
            channel.disconnect();
            session.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void autoScaling() {
        System.out.println("Starting auto-scaling...");

        try {
            String scriptResult = executeShellScript("/home/ec2-user/autoscaling.sh");
            String[] results = scriptResult.split("\n");

            int slots = Integer.parseInt(results[0].trim());
            int jobs = Integer.parseInt(results[1].trim());
            List<String> assignedNodes = Arrays.asList(results[2].trim().split(","));

            if (jobs > slots) {
                System.out.println("Scale out: Creating a new instance...");
                createInstance("ami-064d68c52313d6d29"); // worker 이미지 기반 인스턴스 생성
            } else if (jobs < slots) {
                System.out.println("Scale in: Stopping unnecessary instances...");
                scaleInInstances(assignedNodes);
            } else {
                System.out.println("System stable: No scaling required.");
            }
        } catch (Exception e) {
            System.out.println("Error in auto-scaling: " + e.getMessage());
        }
    }

    private static String executeShellScript(String scriptName) throws Exception {
        String host = "ec2-3-38-98-219.ap-northeast-2.compute.amazonaws.com";
        String user = "ec2-user";
        String privateKeyPath = "C:\\Users\\heeju\\heeju-key.pem";

        JSch jsch = new JSch();
        jsch.addIdentity(privateKeyPath);

        Session session = jsch.getSession(user, host, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("/bin/bash " + scriptName);
        InputStream in = channel.getInputStream();
        channel.connect();

        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            output.append(new String(buffer, 0, bytesRead));
        }

        channel.disconnect();
        session.disconnect();

        return output.toString().trim();
    }

    private static void scaleInInstances(List<String> assignedNodes) {
        DescribeInstancesRequest request = new DescribeInstancesRequest()
                .withFilters(new Filter().withName("instance-state-name").withValues("running"));
        DescribeInstancesResult response = ec2.describeInstances(request);

        List<Instance> runningInstances = new ArrayList<>();
        for (Reservation reservation : response.getReservations()) {
            runningInstances.addAll(reservation.getInstances());
        }

        // 이미 중지된 인스턴스를 추적하기 위한 Set 생성
        Set<String> stoppedInstances = new HashSet<>();

        // 인스턴스 순회
        for (Instance instance : runningInstances) {
            String instanceId = instance.getInstanceId();
            String privateIP = instance.getPrivateIpAddress();

            // Main 노드와 Assigned 노드를 제외하고 처리
            if (!assignedNodes.contains(privateIP)
                    && !instanceId.equals("i-0e2da5fd0cf96ff18")  // Main 노드 제외
                    && !stoppedInstances.contains(instanceId)) {
                stopInstance(instanceId);
                stoppedInstances.add(instanceId); // 중지된 인스턴스 기록
            }
        }
    }


}
