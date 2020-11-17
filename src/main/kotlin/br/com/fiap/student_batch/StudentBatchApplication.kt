package br.com.fiap.student_batch

import antlr.StringUtils
import br.com.fiap.student_batch.entity.FileLineDTO
import br.com.fiap.student_batch.entity.Student
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import javax.sql.DataSource


@SpringBootApplication
@EnableBatchProcessing
class StudentBatchApplication {

	var logger: Logger = LoggerFactory.getLogger(StudentBatchApplication::class.java)

	@Bean
	fun itemReader(@Value("\${input.file}") resource: Resource): FlatFileItemReader<FileLineDTO>? {

		try {
			return FlatFileItemReaderBuilder<FileLineDTO>()
					.name("Lendo Arquivo de alunos")
					.resource(resource)
					.lineMapper { line, lineNumber ->
						FileLineDTO(line = line)
					}
					.targetType(FileLineDTO::class.java)
					.build()
		} catch (e: Exception) {
			throw Exception(e.message)
		}
	}

	@Bean
	fun itemProcessor(): ItemProcessor<FileLineDTO?, Student?>? {
		return ItemProcessor<FileLineDTO?, Student?> { fileLineDTO ->

			if(fileLineDTO.line.isNotEmpty() && fileLineDTO.line.indexOf("--") == -1 && fileLineDTO.line.indexOf("\u001A") == -1) {
				println("formatting line $fileLineDTO")

				var name = fileLineDTO.line.substring(0, 40).trim()
				var ra = fileLineDTO.line.substring(41, 48).trim()
				var className = fileLineDTO.line.substring(49, (fileLineDTO.line.length)).trim()

				Student(name = name, ra = ra, className = className, addresId = null, id = null, cardId = null, cpf = null)
			} else {
				println("invalid line $fileLineDTO")
				null
			}

		}
	}

	@Bean
	fun itemWriter(dataSource: DataSource): JdbcBatchItemWriter<Student?>? {
		return JdbcBatchItemWriterBuilder<Student>()
				.dataSource(dataSource)
				.sql("insert into STUDENT (name, ra, class_name) values (:name, :ra, :className)")
				.beanMapped()
				.build()
	}

	@Bean
	fun step(stepBuilderFactory: StepBuilderFactory,
			 itemReader: ItemReader<FileLineDTO>,
			 itemProcessor: ItemProcessor<FileLineDTO, Student>?,
			 itemWriter: ItemWriter<Student>): Step? {
		return stepBuilderFactory["Step Chunck - Processar arquivo de alunos"]
				.chunk<FileLineDTO, Student?>(100)
				.reader(itemReader)
				.processor(itemProcessor!!)
				.writer(itemWriter)
				.allowStartIfComplete(true)
				.build()
	}

	@Bean
	fun job(jobBuilderFactory: JobBuilderFactory, step: Step): Job? {
		return jobBuilderFactory["Job - Importar arquivo de alunos"]
				.start(step)
				.build()
	}

}

fun main(args: Array<String>) {
	runApplication<StudentBatchApplication>(*args)
}
