package com.example

import scala.concurrent.{ ExecutionContext, Future }

class StudentsDAO() {
  def getStudent(id: Int)(implicit ec: ExecutionContext): Future[Student] = Future {
    Student(id, None)
  }

  def getStudentGrades(studentId: Int)(implicit ec: ExecutionContext): Future[List[Double]] = Future {
    if (studentId == 1) List(5.0, 5.0, 4.8, 5.0, 3.3)
    else List(4.0, 3.7, 5.0, 1.2, 0.0, 4.3)
  }
}
