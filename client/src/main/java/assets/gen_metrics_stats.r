pdf()
files <- dir(pattern="metrics.*csv")

s_script <- vector(mode="integer")
s_ins <- vector(mode="integer")
s_del <- vector(mode="integer")
s_up <- vector(mode="integer")
s_mov <- vector(mode="integer")
t_total <- vector(mode="integer")


for (file in files) {
	d <- read.csv(file, header=T, sep=";")
	length(d[[3]])
	s_script <- cbind(s_script, d[[3]])
	s_ins <- cbind(s_ins, d[[3]])
	s_del <- cbind(s_del, d[[4]])
	s_up <- cbind(s_up, d[[5]])
	s_mov <- cbind(s_mov, d[[6]])
	t_total <- cbind(t_total, d[[11]])
}
boxplot(s_script, main="Edit script size")
boxplot(s_ins, main="Insert actions")
boxplot(s_del, main="Delete actions")
boxplot(s_up, main="Update actions")
boxplot(s_mov, main="Move actions")
boxplot(t_total, main="Total time")