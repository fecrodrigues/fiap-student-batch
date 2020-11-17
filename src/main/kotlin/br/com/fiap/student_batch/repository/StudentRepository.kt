package br.com.fiap.student_batch.repository

import br.com.fiap.student_batch.entity.Student
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigInteger

interface StudentRepository : JpaRepository<Student, BigInteger>