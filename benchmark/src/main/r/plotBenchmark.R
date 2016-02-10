library(ggplot2)
folder <- commandArgs()[length(commandArgs())]
pdf(file = file.path(folder, "all_results.pdf"))
files <- list.files(path = folder, pattern = "*.csv", full.names = T)
for (f in files) {
  tmp <- read.csv(f)
  tmp$timestamp <- basename(f)
  if (exists("d")) {
    d <- rbind(d, tmp)
  } else {
    d <- tmp
  }
}
ggplot(d, aes(timestamp, Score)) + geom_jitter(position=position_jitter(0.2))