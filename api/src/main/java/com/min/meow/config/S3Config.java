package com.min.meow.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 설정
 * S3Client: 파일 삭제, 존재 여부 확인 등 일반 S3 작업용
 * S3Presigner: Presigned URL 생성용 (클라이언트 직접 업로드)
 * application.yml에서 AWS 자격 증명을 읽어와서 사용합니다.
 * 환경 변수: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
 */
@Configuration
public class S3Config {

    @Value("${cloud.aws.region:ap-northeast-2}")
    private String region;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    /**
     * AWS 자격 증명 Provider 생성
     * application.yml의 설정값을 사용
     */
    private StaticCredentialsProvider credentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return StaticCredentialsProvider.create(credentials);
    }

    /**
     * S3Client 빈
     * - 파일 삭제 (deleteObject)
     * - 파일 존재 확인 (headObject)
     */
    @Bean
    public S3Client s3Client(){
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    /**
     * S3Presigner 빈
     * - Presigned URL 생성 (클라이언트가 S3에 직접 업로드할 수 있는 임시 URL)
     * - 서버를 거치지 않고 S3에 직접 업로드하여 서버 트래픽 비용 절감
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }
}
