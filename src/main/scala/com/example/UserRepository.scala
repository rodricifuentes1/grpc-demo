package com.example

import monix.eval.Task

class UserRepository {
  def getUserTask(id: String): Task[User] = Task {
    if (id == "1") User("1", "admin")
    else User("2", "user")
  }
}
