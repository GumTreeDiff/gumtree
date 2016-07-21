# This file is part of GumTree.
#
# GumTree is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# GumTree is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
#
# Copyright 2016 Jean-Rémy Falleri <jr.falleri@gmail.com>
# Copyright 2016 Floréal Morandat <florealm@gmail.com>

library(ggplot2)
library(plyr)
folder <- commandArgs()[length(commandArgs())]
pdf(file = file.path(folder, "all_results.pdf"))
files <- list.files(path = folder, pattern = "*.csv", full.names = T)


d <- ldply(files, function (filename)
           cbind(read.csv(filename), timestamp =
                 paste(as.POSIXct(as.numeric(gsub("^.*results_(\\d*).csv", "\\1", filename))/1000, origin="1970-01-01"))))
d$name <- gsub('^.*perfs_(.*)_v0_(.*).xml$', '\\1_\\2', d$Param..refPath)

# according to my office mate we should change the size of each line from 0.5 to 0.1
# but I don't know how to do this (size=seq(0.5, 0.1) does not work)
ggplot(d, aes(timestamp, Score, group=d$name, colour=name)) +
  geom_point() +
  geom_line() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1))
