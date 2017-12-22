package com.example

import monix.eval.Task

class StudentsRepo() {

  private lazy val dao = new StudentsDAO()

  def getStudent(id: Int): Task[Student] = {
    // methods dao.getStudent and dao.getStudent grades need an implicit execution context (because they use futures)
    // Task.deferFutureAction provides task scheduler (monix execution context) and passes it into dao methods
    val getStudentTask       = Task.deferFutureAction(implicit scheduler => dao.getStudent(id))
    val getStudentGradesTask = Task.deferFutureAction(implicit scheduler => dao.getStudentGrades(id))
    for {
      student <- getStudentTask
      grades  <- getStudentGradesTask
    } yield student.copy(grades = Some(grades))
  }
}
