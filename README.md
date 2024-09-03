# GumTree

An awesome code differencing tool that you can [integrate with Git](https://github.com/GumTreeDiff/gumtree/wiki/VCS-Integration)!

## Status

![Build and Test GumTree](https://github.com/GumTreeDiff/gumtree/workflows/Build,%20Test%20and%20Deploy%20GumTree/badge.svg?branch=main)

## Description

GumTree is a syntax-aware diff tool. It improves text-based diff tools in two important ways:
* the edit actions are always aligned with the syntax,
* it can detect moved or renamed elements in addition of deleted and inserted code.

## Documentation

To use GumTree, you can start by consulting the [Getting Started](https://github.com/GumTreeDiff/gumtree/wiki/Getting-Started) page from our [wiki](https://github.com/GumTreeDiff/gumtree/wiki). If you have a question to ask, please use GitHub's [discussions](https://github.com/GumTreeDiff/gumtree/discussions) instead of opening an issue.

## Screenshots

### The directory diff viewer

![Directory comparator view](https://github.com/GumTreeDiff/gumtree/raw/main/doc/screenshots/screenshot-0.png)

### The file diff viewer

![Diff view on a CSS file](https://github.com/GumTreeDiff/gumtree/raw/main/doc/screenshots/screenshot-1.png)

![Diff view on a Java file](https://github.com/GumTreeDiff/gumtree/raw/main/doc/screenshots/screenshot-2.png)

## Supported languages

We already deal with a wide range of languages: C, Java, JavaScript, Python, R, Ruby. Click [here](https://github.com/GumTreeDiff/gumtree/wiki/Languages) for more details about the language we support.

More languages are coming soon, if you want to help contact [me](http://www.labri.fr/perso/falleri).

## Citing GumTree

We are researchers, therefore if you use GumTree in an academic work we would be really glad if you cite the relevant articles among the ones in the following bibtex:

```bibtex
@inproceedings{DBLP:conf/kbse/FalleriMBMM14,
  author    = {Jean{-}R{\'{e}}my Falleri and
               Flor{\'{e}}al Morandat and
               Xavier Blanc and
               Matias Martinez and
               Martin Monperrus},
  title     = {Fine-grained and accurate source code differencing},
  booktitle = {{ACM/IEEE} International Conference on Automated Software Engineering,
               {ASE} '14, Vasteras, Sweden - September 15 - 19, 2014},
  pages     = {313--324},
  year      = {2014},
  url       = {http://doi.acm.org/10.1145/2642937.2642982},
  doi       = {10.1145/2642937.2642982}
}

@article{DBLP:journals/tse/MartinezFM23,
  author       = {Matias Martinez and
                  Jean{-}R{\'{e}}my Falleri and
                  Martin Monperrus},
  title        = {Hyperparameter Optimization for {AST} Differencing},
  journal      = {{IEEE} Trans. Software Eng.},
  volume       = {49},
  number       = {10},
  pages        = {4814--4828},
  year         = {2023},
  url          = {https://doi.org/10.1109/TSE.2023.3315935},
  doi          = {10.1109/TSE.2023.3315935},
  timestamp    = {Thu, 09 Nov 2023 21:13:48 +0100},
  biburl       = {https://dblp.org/rec/journals/tse/MartinezFM23.bib},
  bibsource    = {dblp computer science bibliography, https://dblp.org}
}

@inproceedings{DBLP:conf/icse/FalleriM24,
  author       = {Jean{-}R{\'{e}}my Falleri and
                  Matias Martinez},
  title        = {Fine-grained, accurate and scalable source differencing},
  booktitle    = {Proceedings of the 46th {IEEE/ACM} International Conference on Software
                  Engineering, {ICSE} 2024, Lisbon, Portugal, April 14-20, 2024},
  pages        = {231:1--231:12},
  publisher    = {{ACM}},
  year         = {2024},
  url          = {https://doi.org/10.1145/3597503.3639148},
  doi          = {10.1145/3597503.3639148},
  timestamp    = {Mon, 24 Jun 2024 15:20:25 +0200},
  biburl       = {https://dblp.org/rec/conf/icse/FalleriM24.bib},
  bibsource    = {dblp computer science bibliography, https://dblp.org}
}
```
