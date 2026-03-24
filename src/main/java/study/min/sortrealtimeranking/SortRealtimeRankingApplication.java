package study.min.sortrealtimeranking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SortRealtimeRankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SortRealtimeRankingApplication.class, args);
    }

}
