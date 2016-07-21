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

library(ggplot2)
folder <- commandArgs()[length(commandArgs())]
pdf(file = file.path(folder, "all_results.pdf"))
files <- list.files(path = folder, pattern = "*.csv", full.names = T)
d <- list()
for (f in files) {
  tmp <- read.csv(f)
  tmp$timestamp <- basename(f)
  d <- rbind(d, tmp)
}
ggplot(d, aes(timestamp, Score)) +
  geom_jitter(position=position_jitter(0.2),
              color=rep(rainbow(5), length(files))) +
  theme(axis.text.x = element_text(angle = 45, hjust = 1))
