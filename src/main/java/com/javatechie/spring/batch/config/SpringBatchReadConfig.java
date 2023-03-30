package com.javatechie.spring.batch.config;

import com.javatechie.spring.batch.entity.Customer;
import com.javatechie.spring.batch.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableBatchProcessing
@AllArgsConstructor
public class SpringBatchReadConfig {

    private JobBuilderFactory jobBuilderFactory;

    private StepBuilderFactory stepBuilderFactory;

    private CustomerRepository customerRepository;

    @Bean
    public RepositoryItemReader<Customer> repositoryReader() {
        RepositoryItemReader<Customer> itemReader = new RepositoryItemReader();
        itemReader.setRepository(customerRepository);
        itemReader.setMethodName("findAll");
        HashMap<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("id", Sort.Direction.DESC);
        itemReader.setSort(sorts);
        return itemReader;
    }

    @Bean
    public CustomerProcessor customerProcessor() {
        return new CustomerProcessor();
    }

    @Bean
    public FlatFileItemWriter<Customer> csvWriter() {
        FlatFileItemWriter<Customer> flatFileItemWriter = new FlatFileItemWriter<>();
        flatFileItemWriter.setResource(new FileSystemResource("src/main/resources/customersResult.csv"));
        DelimitedLineAggregator<Customer> lineAggregator = new DelimitedLineAggregator();
        lineAggregator.setDelimiter(",");

        BeanWrapperFieldExtractor<Customer> fieldSetMapper = new BeanWrapperFieldExtractor<>();
        fieldSetMapper.setNames(new String[]{"id", "firstName", "lastName", "email", "gender", "contactNo", "country", "dob"});
        lineAggregator.setFieldExtractor(fieldSetMapper);
        flatFileItemWriter.setLineAggregator(lineAggregator);
        return flatFileItemWriter;
    }

    //@Bean
    public Step exportStep1() {
        return stepBuilderFactory.get("db-step").<Customer, Customer>chunk(10)
                .reader(repositoryReader())
                .processor(customerProcessor())
                .writer(csvWriter())
                .taskExecutor(readTaskExecutor())
                .build();
    }

    @Bean(name = "exportCustomerJob")
    public Job exportCustomerJob() {
        return jobBuilderFactory.get("getCustomers")
                .incrementer(new RunIdIncrementer())
                .flow(exportStep1())
                .end()
                .build();
    }

    @Bean
    public TaskExecutor readTaskExecutor() {
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
        asyncTaskExecutor.setConcurrencyLimit(10);
        return asyncTaskExecutor;
    }

}
