/*
Copyright 2020 Wes Kaylor

This file is part of CodeGenerator.

CodeGenerator is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CodeGenerator is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with CodeGenerator.  If not, see <http://www.gnu.org/licenses/>.
*/


package codegenerator.generator.utils.multithreading;



import java.util.concurrent.*;

import coreutil.logging.*;



public class ThreadPoolManager {

	// Static members
	static private		 ThreadPoolExecutor		s_threadPool	= null;


	//===========================================
	static public boolean Init() {
		try {
			if (s_threadPool == null) {
				int t_threadCount = Runtime.getRuntime().availableProcessors();
				s_threadPool = new ThreadPoolExecutor(t_threadCount,
													  t_threadCount,
													  0,
													  TimeUnit.SECONDS,
													  new LinkedBlockingQueue<Runnable>());	// If the queue is limited in any way, then if you create and insert more tasks than the fixed queue size it causes a runtime exception to be thrown.
			}

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("ThreadPoolManager.Init() failed with error: ", t_error);
			return false;
		}
	}


	//===========================================
	static public void AddTask(Runnable t_task) {
		s_threadPool.execute(t_task);
	}


	//===========================================
	static public boolean Shutdown() {
		try {
			// This sucks!!  Now that I'm doing nested file blocks, you can't call shutdown() until the queue is empty because and file task in the queue can itself create one or more new file tasks.
			// If you shutdown() and the end of the initial Evaluate(), you've only created the first order file tasks but now the pool queue is blocked.  If any of the file tasks in the queue generate a new nested file task, that task is rejected by the now "shutdown" thread pool.
			BlockingQueue<Runnable> t_queue = s_threadPool.getQueue();
			while(!t_queue.isEmpty()){}

			s_threadPool.shutdown();
			s_threadPool.awaitTermination(1, TimeUnit.MINUTES);

			return true;
		}
		catch (Throwable t_error) {
			Logger.LogException("ThreadPoolManager.Shutdown() failed with error: ", t_error);
			return false;
		}
	}
}
