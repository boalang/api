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