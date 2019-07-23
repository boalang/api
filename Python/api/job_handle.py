class JobHandle:
	"""A class for handling jobs sent to the framework

	Attributes:
		client (BoaClient): the xmlrpc client
		id (int): the jobs id
		date (str): the date and time the job was submitted
		dataset (dict): the dataset used to executed the job
		exec_status (str): the execution status for the job
		compiler_status (str): the compiler status for the job
	"""

	def __init__(self, client, id, date, dataset, compiler_status, exec_status):
		self.client = client
		self.id = id
		self.date = date
		self.dataset = dataset
		self.compiler_status = compiler_status
		self.exec_status = exec_status

	def __str__(self):
		"""string output for a job"""
		return str('id: ' + str(self.id) + ', date:' + str(self.date) + 
		', dataset:' + str(self.dataset) + ', compiler_status: (' + str(self.compiler_status) + ')' 
		+', execution_status: (' + str(self.exec_status) + ')')
	
	def stop(self):
		return self.client.stop(self)
		
	def resubmit(self):
		return self.client.resubmit(self)
		
	def delete(self):
		return self.client.delete(self)

	def get_url(self):
		return self.client.get_url(self)

	def set_public(self, status):
		return self.client.set_public(self, status)

	def get_public(self):
		return self.client.get_public(self)

	def get_public_url(self):
		return self.client.get_public_url(self)

	def get_source(self):
		return self.client.get_source(self)

	def get_compiler_errors(self):
		return self.client.get_compiler_errors(self)

	def get_output(self, start=0, length=1000):
		return self.client.get_output(self, start, length)
		
	def refresh(self):
		job = self.client.get_job(self)
		self.compiler_status = job['compiler_status']
		self.exec_status = job['execution_status']
		self.date = job['date']
        