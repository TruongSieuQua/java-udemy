var FI_G,
  ForgettingCurves,
  Item,
  MAX_AF,
  MAX_GRADE,
  MIN_AF,
  NOTCH_AF,
  OFM,
  RANGE_AF,
  RANGE_REPETITION,
  RFM,
  THRESHOLD_RECALL,
  error,
  exponentialRegression,
  fixedPointPowerLawRegression,
  linearRegression,
  linearRegressionThroughOrigin,
  main,
  mse,
  powerLawModel,
  powerLawRegression,
  sum,
  __bind = function (fn, me) {
    return function () {
      return fn.apply(me, arguments);
    };
  };

const SM = (function () {
  function SM() {
    this.data = __bind(this.data, this);
    this.discard = __bind(this.discard, this);
    this._update = __bind(this._update, this);
    this.answer = __bind(this.answer, this);
    this.nextItem = __bind(this.nextItem, this);
    this.addItem = __bind(this.addItem, this);
    this._findIndexToInsert = __bind(this._findIndexToInsert, this);
    this.requestedFI = 10;
    this.intervalBase = 3 * 60 * 60 * 1000;
    this.q = [];
    this.fi_g = new FI_G(this);
    this.forgettingCurves = new ForgettingCurves(this);
    this.rfm = new RFM(this);
    this.ofm = new OFM(this);
  }

  SM.prototype._findIndexToInsert = function (item, r) {
    var i, v, _i, _ref, _results;
    if (r == null) {
      r = function () {
        _results = [];
        for (
          var _i = 0, _ref = this.q.length;
          0 <= _ref ? _i < _ref : _i > _ref;
          0 <= _ref ? _i++ : _i--
        ) {
          _results.push(_i);
        }
        return _results;
      }.apply(this);
    }
    if (r.length === 0) {
      return 0;
    }
    v = item.dueDate;
    i = Math.floor(r.length / 2);
    if (r.length === 1) {
      if (v < this.q[r[i]].dueDate) {
        return r[i];
      } else {
        return r[i] + 1;
      }
    }
    return this._findIndexToInsert(
      item,
      v < this.q[r[i]].dueDate ? r.slice(0, i) : r.slice(i),
    );
  };

  SM.prototype.addItem = function (value) {
    var item;
    item = new Item(this, value);
    return this.q.splice(this._findIndexToInsert(item), 0, item);
  };

  SM.prototype.nextItem = function (isAdvanceable) {
    if (isAdvanceable == null) {
      isAdvanceable = false;
    }
    if (0 === this.q.length) {
      return null;
    }
    if (isAdvanceable || this.q[0].dueDate < new Date()) {
      return this.q[0];
    }
    return null;
  };

  SM.prototype.answer = function (grade, item, now) {
    if (now == null) {
      now = new Date();
    }
    this._update(grade, item, now);
    this.discard(item);
    return this.q.splice(this._findIndexToInsert(item), 0, item);
  };

  SM.prototype._update = function (grade, item, now) {
    if (now == null) {
      now = new Date();
    }
    if (item.repetition >= 0) {
      this.forgettingCurves.registerPoint(grade, item, now);
      this.ofm.update();
      this.fi_g.update(grade, item, now);
    }
    return item.answer(grade, now);
  };

  SM.prototype.discard = function (item) {
    var index;
    index = this.q.indexOf(item);
    if (index >= 0) {
      return this.q.splice(index, 1);
    }
  };

  SM.prototype.data = function () {
    var item;
    return {
      requestedFI: this.requestedFI,
      intervalBase: this.intervalBase,
      q: function () {
        var _i, _len, _ref, _results;
        _ref = this.q;
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          item = _ref[_i];
          _results.push(item.data());
        }
        return _results;
      }.call(this),
      fi_g: this.fi_g.data(),
      forgettingCurves: this.forgettingCurves.data(),
      version: 1,
    };
  };

  SM.load = function (data) {
    var d, sm;
    sm = new SM();
    sm.requestedFI = data.requestedFI;
    sm.intervalBase = data.intervalBase;
    sm.q = (function () {
      var _i, _len, _ref, _results;
      _ref = data.q;
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        d = _ref[_i];
        _results.push(Item.load(sm, d));
      }
      return _results;
    })();
    sm.fi_g = FI_G.load(sm, data.fi_g);
    sm.forgettingCurves = ForgettingCurves.load(sm, data.forgettingCurves);
    sm.ofm.update();
    return sm;
  };

  return SM;
})();

RANGE_AF = 20;

RANGE_REPETITION = 20;

MIN_AF = 1.2;

NOTCH_AF = 0.3;

MAX_AF = MIN_AF * NOTCH_AF * (RANGE_AF - 1);

MAX_GRADE = 5;

THRESHOLD_RECALL = 3;

Item = (function () {
  var MAX_AFS_COUNT;

  MAX_AFS_COUNT = 30;

  function Item(sm, value) {
    this.sm = sm;
    this.value = value;
    this.data = __bind(this.data, this);
    this.answer = __bind(this.answer, this);
    this._updateAF = __bind(this._updateAF, this);
    this._I = __bind(this._I, this);
    this.afIndex = __bind(this.afIndex, this);
    this.af = __bind(this.af, this);
    this.uf = __bind(this.uf, this);
    this.interval = __bind(this.interval, this);
    this.lapse = 0;
    this.repetition = -1;
    this.of = 1;
    this.optimumInterval = this.sm.intervalBase;
    this.dueDate = new Date(0);
    this._afs = [];
  }

  Item.prototype.interval = function (now) {
    if (now == null) {
      now = new Date();
    }
    if (this.previousDate == null) {
      return this.sm.intervalBase;
    }
    return now - this.previousDate;
  };

  Item.prototype.uf = function (now) {
    if (now == null) {
      now = new Date();
    }
    return this.interval(now) / (this.optimumInterval / this.of);
  };

  Item.prototype.af = function (value) {
    var a;
    if (value == null) {
      value = void 0;
    }
    if (value == null) {
      return this._af;
    }
    a = Math.round((value - MIN_AF) / NOTCH_AF);
    return (this._af = Math.max(
      MIN_AF,
      Math.min(MAX_AF, MIN_AF + a * NOTCH_AF),
    ));
  };

  Item.prototype.afIndex = function () {
    var afs, i, _i, _results;
    afs = (function () {
      var _i, _results;
      _results = [];
      for (
        i = _i = 0;
        0 <= RANGE_AF ? _i < RANGE_AF : _i > RANGE_AF;
        i = 0 <= RANGE_AF ? ++_i : --_i
      ) {
        _results.push(MIN_AF + i * NOTCH_AF);
      }
      return _results;
    })();
    return function () {
      _results = [];
      for (
        var _i = 0;
        0 <= RANGE_AF ? _i < RANGE_AF : _i > RANGE_AF;
        0 <= RANGE_AF ? _i++ : _i--
      ) {
        _results.push(_i);
      }
      return _results;
    }
      .apply(this)
      .reduce(
        (function (_this) {
          return function (a, b) {
            if (Math.abs(_this.af() - afs[a]) < Math.abs(_this.af() - afs[b])) {
              return a;
            } else {
              return b;
            }
          };
        })(this),
      );
  };

  Item.prototype._I = function (now) {
    var of_;
    if (now == null) {
      now = new Date();
    }
    of_ = this.sm.ofm.of(
      this.repetition,
      this.repetition === 0 ? this.lapse : this.afIndex(),
    );
    this.of = Math.max(
      1,
      (of_ - 1) * (this.interval(now) / this.optimumInterval) + 1,
    );
    this.optimumInterval = Math.round(this.optimumInterval * this.of);
    this.previousDate = now;
    return (this.dueDate = new Date(now.getTime() + this.optimumInterval));
  };

  Item.prototype._updateAF = function (grade, now) {
    var correctedUF, estimatedAF, estimatedFI, _i, _ref, _results;
    if (now == null) {
      now = new Date();
    }
    estimatedFI = Math.max(1, this.sm.fi_g.fi(grade));
    correctedUF = this.uf(now) * (this.sm.requestedFI / estimatedFI);
    estimatedAF =
      this.repetition > 0
        ? this.sm.ofm.af(this.repetition, correctedUF)
        : Math.max(MIN_AF, Math.min(MAX_AF, correctedUF));
    this._afs.push(estimatedAF);
    this._afs = this._afs.slice(Math.max(0, this._afs.length - MAX_AFS_COUNT));
    return this.af(
      sum(
        this._afs.map(function (a, i) {
          return a * (i + 1);
        }),
      ) /
        sum(
          function () {
            _results = [];
            for (
              var _i = 1, _ref = this._afs.length;
              1 <= _ref ? _i <= _ref : _i >= _ref;
              1 <= _ref ? _i++ : _i--
            ) {
              _results.push(_i);
            }
            return _results;
          }.apply(this),
        ),
    );
  };

  Item.prototype.answer = function (grade, now) {
    if (now == null) {
      now = new Date();
    }
    if (this.repetition >= 0) {
      this._updateAF(grade, now);
    }
    if (grade >= THRESHOLD_RECALL) {
      if (this.repetition < RANGE_REPETITION - 1) {
        this.repetition++;
      }
      return this._I(now);
    } else {
      if (this.lapse < RANGE_AF - 1) {
        this.lapse++;
      }
      this.optimumInterval = this.sm.intervalBase;
      this.previousDate = null;
      this.dueDate = now;
      return (this.repetition = -1);
    }
  };

  Item.prototype.data = function () {
    return {
      value: this.value,
      repetition: this.repetition,
      lapse: this.lapse,
      of: this.of,
      optimumInterval: this.optimumInterval,
      dueDate: this.dueDate,
      previousDate: this.previousDate,
      _afs: this._afs,
    };
  };

  Item.load = function (sm, data) {
    var item, k, v;
    item = new Item(sm);
    for (k in data) {
      v = data[k];
      item[k] = v;
    }
    item.dueDate = new Date(item.dueDate);
    if (item.previousDate != null) {
      item.previousDate = new Date(item.previousDate);
    }
    return item;
  };

  return Item;
})();

FI_G = (function () {
  var GRADE_OFFSET, MAX_POINTS_COUNT;

  MAX_POINTS_COUNT = 5000;

  GRADE_OFFSET = 1;

  function FI_G(sm, points) {
    var p, _i, _len, _ref;
    this.sm = sm;
    this.points = points != null ? points : void 0;
    this.data = __bind(this.data, this);
    this.grade = __bind(this.grade, this);
    this.fi = __bind(this.fi, this);
    this.update = __bind(this.update, this);
    this._registerPoint = __bind(this._registerPoint, this);
    if (this.points == null) {
      this.points = [];
      _ref = [
        [0, MAX_GRADE],
        [100, 0],
      ];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        p = _ref[_i];
        this._registerPoint(p[0], p[1]);
      }
    }
  }

  FI_G.prototype._registerPoint = function (fi, g) {
    this.points.push([fi, g + GRADE_OFFSET]);
    return (this.points = this.points.slice(
      Math.max(0, this.points.length - MAX_POINTS_COUNT),
    ));
  };

  FI_G.prototype.update = function (grade, item, now) {
    var expectedFI;
    if (now == null) {
      now = new Date();
    }
    expectedFI = (function (_this) {
      return function () {
        return (item.uf(now) / item.of) * _this.sm.requestedFI;

        /* A way to get the expected forgetting index using a forgetting curve
          curve = @sm.forgettingCurves.curves[item.repetition][item.afIndex()]
          uf = curve.uf (100 - @sm.requestedFI)
          return 100 - curve.retention (item.uf() / uf)
           */
      };
    })(this);
    this._registerPoint(expectedFI(), grade);
    return (this._graph = null);
  };

  FI_G.prototype.fi = function (grade) {
    var _ref;
    if (this._graph == null) {
      this._graph = exponentialRegression(this.points);
    }
    return Math.max(
      0,
      Math.min(
        100,
        (_ref = this._graph) != null ? _ref.x(grade + GRADE_OFFSET) : void 0,
      ),
    );
  };

  FI_G.prototype.grade = function (fi) {
    var _ref;
    if (this._graph == null) {
      this._graph = exponentialRegression(this.points);
    }
    return ((_ref = this._graph) != null ? _ref.y(fi) : void 0) - GRADE_OFFSET;
  };

  FI_G.prototype.data = function () {
    return {
      points: this.points,
    };
  };

  FI_G.load = function (sm, data) {
    return new FI_G(sm, data.points);
  };

  return FI_G;
})();

ForgettingCurves = (function () {
  var FORGOTTEN, ForgettingCurve, REMEMBERED;

  FORGOTTEN = 1;

  REMEMBERED = 100 + FORGOTTEN;

  function ForgettingCurves(sm, points) {
    var a, i, p, partialPoints, r;
    this.sm = sm;
    if (points == null) {
      points = void 0;
    }
    this.data = __bind(this.data, this);
    this.registerPoint = __bind(this.registerPoint, this);
    this.curves = function () {
      var _i, _results;
      _results = [];
      for (
        r = _i = 0;
        0 <= RANGE_REPETITION ? _i < RANGE_REPETITION : _i > RANGE_REPETITION;
        r = 0 <= RANGE_REPETITION ? ++_i : --_i
      ) {
        _results.push(
          function () {
            var _j, _results1;
            _results1 = [];
            for (
              a = _j = 0;
              0 <= RANGE_AF ? _j < RANGE_AF : _j > RANGE_AF;
              a = 0 <= RANGE_AF ? ++_j : --_j
            ) {
              partialPoints =
                points != null
                  ? points[r][a]
                  : ((p =
                      r > 0
                        ? function () {
                            var _k, _results2;
                            _results2 = [];
                            for (i = _k = 0; _k <= 20; i = ++_k) {
                              _results2.push([
                                MIN_AF + NOTCH_AF * i,
                                Math.min(
                                  REMEMBERED,
                                  Math.exp(
                                    (-(r + 1) / 200) *
                                      (i - a * Math.sqrt(2 / (r + 1))),
                                  ) *
                                    (REMEMBERED - this.sm.requestedFI),
                                ),
                              ]);
                            }
                            return _results2;
                          }.call(this)
                        : function () {
                            var _k, _results2;
                            _results2 = [];
                            for (i = _k = 0; _k <= 20; i = ++_k) {
                              _results2.push([
                                MIN_AF + NOTCH_AF * i,
                                Math.min(
                                  REMEMBERED,
                                  Math.exp(
                                    (-1 / (10 + 1 * (a + 1))) *
                                      (i - Math.pow(a, 0.6)),
                                  ) *
                                    (REMEMBERED - this.sm.requestedFI),
                                ),
                              ]);
                            }
                            return _results2;
                          }.call(this)),
                    [[0, REMEMBERED]].concat(p));
              _results1.push(new ForgettingCurve(partialPoints));
            }
            return _results1;
          }.call(this),
        );
      }
      return _results;
    }.call(this);
  }

  ForgettingCurves.prototype.registerPoint = function (grade, item, now) {
    var afIndex;
    if (now == null) {
      now = new Date();
    }
    afIndex = item.repetition > 0 ? item.afIndex() : item.lapse;
    return this.curves[item.repetition][afIndex].registerPoint(
      grade,
      item.uf(now),
    );
  };

  ForgettingCurves.prototype.data = function () {
    var a, r;
    return {
      points: function () {
        var _i, _results;
        _results = [];
        for (
          r = _i = 0;
          0 <= RANGE_REPETITION ? _i < RANGE_REPETITION : _i > RANGE_REPETITION;
          r = 0 <= RANGE_REPETITION ? ++_i : --_i
        ) {
          _results.push(
            function () {
              var _j, _results1;
              _results1 = [];
              for (
                a = _j = 0;
                0 <= RANGE_AF ? _j < RANGE_AF : _j > RANGE_AF;
                a = 0 <= RANGE_AF ? ++_j : --_j
              ) {
                _results1.push(this.curves[r][a].points);
              }
              return _results1;
            }.call(this),
          );
        }
        return _results;
      }.call(this),
    };
  };

  ForgettingCurves.load = function (sm, data) {
    return new ForgettingCurves(sm, data.points);
  };

  ForgettingCurve = (function () {
    var MAX_POINTS_COUNT;

    MAX_POINTS_COUNT = 500;

    function ForgettingCurve(points) {
      this.points = points;
      this.uf = __bind(this.uf, this);
      this.retention = __bind(this.retention, this);
      this.registerPoint = __bind(this.registerPoint, this);
    }

    ForgettingCurve.prototype.registerPoint = function (grade, uf) {
      var isRemembered;
      isRemembered = grade >= THRESHOLD_RECALL;
      this.points.push([uf, isRemembered ? REMEMBERED : FORGOTTEN]);
      this.points = this.points.slice(
        Math.max(0, this.points.length - MAX_POINTS_COUNT),
      );
      return (this._curve = null);
    };

    ForgettingCurve.prototype.retention = function (uf) {
      if (this._curve == null) {
        this._curve = exponentialRegression(this.points);
      }
      return (
        Math.max(FORGOTTEN, Math.min(this._curve.y(uf), REMEMBERED)) - FORGOTTEN
      );
    };

    ForgettingCurve.prototype.uf = function (retention) {
      if (this._curve == null) {
        this._curve = exponentialRegression(this.points);
      }
      return Math.max(0, this._curve.x(retention + FORGOTTEN));
    };

    return ForgettingCurve;
  })();

  return ForgettingCurves;
})();

RFM = (function () {
  function RFM(sm) {
    this.sm = sm;
    this.rf = __bind(this.rf, this);
  }

  RFM.prototype.rf = function (repetition, afIndex) {
    return this.sm.forgettingCurves.curves[repetition][afIndex].uf(
      100 - this.sm.requestedFI,
    );
  };

  return RFM;
})();

OFM = (function () {
  var INITIAL_REP_VALUE, afFromIndex, repFromIndex;

  INITIAL_REP_VALUE = 1;

  afFromIndex = function (a) {
    return a * NOTCH_AF + MIN_AF;
  };

  repFromIndex = function (r) {
    return r + INITIAL_REP_VALUE;
  };

  function OFM(sm) {
    this.sm = sm;
    this.af = __bind(this.af, this);
    this.of = __bind(this.of, this);
    this.update = __bind(this.update, this);
    this.update();
  }

  OFM.prototype.update = function () {
    var a, decay, dfs, ofm0, r;
    dfs = function () {
      var _i, _results;
      _results = [];
      for (
        a = _i = 0;
        0 <= RANGE_AF ? _i < RANGE_AF : _i > RANGE_AF;
        a = 0 <= RANGE_AF ? ++_i : --_i
      ) {
        _results.push(
          fixedPointPowerLawRegression(
            function () {
              var _j, _results1;
              _results1 = [];
              for (
                r = _j = 1;
                1 <= RANGE_REPETITION
                  ? _j < RANGE_REPETITION
                  : _j > RANGE_REPETITION;
                r = 1 <= RANGE_REPETITION ? ++_j : --_j
              ) {
                _results1.push([repFromIndex(r), this.sm.rfm.rf(r, a)]);
              }
              return _results1;
            }.call(this),
            [repFromIndex(1), afFromIndex(a)],
          ).b,
        );
      }
      return _results;
    }.call(this);
    dfs = (function () {
      var _i, _results;
      _results = [];
      for (
        a = _i = 0;
        0 <= RANGE_AF ? _i < RANGE_AF : _i > RANGE_AF;
        a = 0 <= RANGE_AF ? ++_i : --_i
      ) {
        _results.push(afFromIndex(a) / Math.pow(2, dfs[a]));
      }
      return _results;
    })();
    decay = linearRegression(
      (function () {
        var _i, _results;
        _results = [];
        for (
          a = _i = 0;
          0 <= RANGE_AF ? _i < RANGE_AF : _i > RANGE_AF;
          a = 0 <= RANGE_AF ? ++_i : --_i
        ) {
          _results.push([a, dfs[a]]);
        }
        return _results;
      })(),
    );
    this._ofm = function (a) {
      /*
          O-Factor (given repetition, A-Factor and D-Factor) would be modeled by power law
          y = a(x/p)^b, a = A-Factor, b = D-Factor, x = repetition, p = 2 #second repetition number
            = (a/p^b)x^b
         */
      var af, b, model;
      af = afFromIndex(a);
      b = Math.log(af / decay.y(a)) / Math.log(repFromIndex(1));
      model = powerLawModel(af / Math.pow(repFromIndex(1), b), b);
      return {
        y: function (r) {
          return model.y(repFromIndex(r));
        },
        x: function (y) {
          return model.x(y) - INITIAL_REP_VALUE;
        },
      };
    };
    ofm0 = exponentialRegression(
      function () {
        var _i, _results;
        _results = [];
        for (
          a = _i = 0;
          0 <= RANGE_AF ? _i < RANGE_AF : _i > RANGE_AF;
          a = 0 <= RANGE_AF ? ++_i : --_i
        ) {
          _results.push([a, this.sm.rfm.rf(0, a)]);
        }
        return _results;
      }.call(this),
    );
    return (this._ofm0 = function (a) {
      return ofm0.y(a);
    });
  };

  OFM.prototype.of = function (repetition, afIndex) {
    return repetition === 0
      ? typeof this._ofm0 === "function"
        ? this._ofm0(afIndex)
        : void 0
      : typeof this._ofm === "function"
      ? this._ofm(afIndex).y(repetition)
      : void 0;
  };

  OFM.prototype.af = function (repetition, of_) {
    var _i, _results;
    return afFromIndex(
      function () {
        _results = [];
        for (
          var _i = 0;
          0 <= RANGE_AF ? _i < RANGE_AF : _i > RANGE_AF;
          0 <= RANGE_AF ? _i++ : _i--
        ) {
          _results.push(_i);
        }
        return _results;
      }
        .apply(this)
        .reduce(
          (function (_this) {
            return function (a, b) {
              if (
                Math.abs(_this.of(repetition, a) - of_) <
                Math.abs(_this.of(repetition, b) - of_)
              ) {
                return a;
              } else {
                return b;
              }
            };
          })(this),
        ),
    );
  };

  return OFM;
})();

sum = function (values) {
  return values.reduce(function (a, b) {
    return a + b;
  });
};

mse = function (y, points) {
  var i;
  return (
    sum(
      (function () {
        var _i, _ref, _results;
        _results = [];
        for (
          i = _i = 0, _ref = points.length;
          0 <= _ref ? _i < _ref : _i > _ref;
          i = 0 <= _ref ? ++_i : --_i
        ) {
          _results.push(Math.pow(y(points[i][0]) - points[i][1], 2));
        }
        return _results;
      })(),
    ) / points.length
  );
};

exponentialRegression = function (points) {
  var X,
    Y,
    a,
    b,
    i,
    logY,
    n,
    p,
    sqSumX,
    sqX,
    sumLogY,
    sumSqX,
    sumX,
    sumXLogY,
    _y;
  n = points.length;
  X = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      p = points[_i];
      _results.push(p[0]);
    }
    return _results;
  })();
  Y = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      p = points[_i];
      _results.push(p[1]);
    }
    return _results;
  })();
  logY = Y.map(Math.log);
  sqX = X.map(function (v) {
    return v * v;
  });
  sumLogY = sum(logY);
  sumSqX = sum(sqX);
  sumX = sum(X);
  sumXLogY = sum(
    (function () {
      var _i, _results;
      _results = [];
      for (i = _i = 0; 0 <= n ? _i < n : _i > n; i = 0 <= n ? ++_i : --_i) {
        _results.push(X[i] * logY[i]);
      }
      return _results;
    })(),
  );
  sqSumX = sumX * sumX;
  a = (sumLogY * sumSqX - sumX * sumXLogY) / (n * sumSqX - sqSumX);
  b = (n * sumXLogY - sumX * sumLogY) / (n * sumSqX - sqSumX);
  _y = function (x) {
    return Math.exp(a) * Math.exp(b * x);
  };
  return {
    y: _y,
    x: function (y) {
      return (-a + Math.log(y)) / b;
    },
    a: Math.exp(a),
    b: b,
    mse: function () {
      return mse(_y, points);
    },
  };
};

linearRegression = function (points) {
  var X, Y, a, b, i, n, p, sqSumX, sqX, sumSqX, sumX, sumXY, sumY;
  n = points.length;
  X = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      p = points[_i];
      _results.push(p[0]);
    }
    return _results;
  })();
  Y = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      p = points[_i];
      _results.push(p[1]);
    }
    return _results;
  })();
  sqX = X.map(function (v) {
    return v * v;
  });
  sumY = sum(Y);
  sumSqX = sum(sqX);
  sumX = sum(X);
  sumXY = sum(
    (function () {
      var _i, _results;
      _results = [];
      for (i = _i = 0; 0 <= n ? _i < n : _i > n; i = 0 <= n ? ++_i : --_i) {
        _results.push(X[i] * Y[i]);
      }
      return _results;
    })(),
  );
  sqSumX = sumX * sumX;
  a = (sumY * sumSqX - sumX * sumXY) / (n * sumSqX - sqSumX);
  b = (n * sumXY - sumX * sumY) / (n * sumSqX - sqSumX);
  return {
    y: function (x) {
      return a + b * x;
    },
    x: function (y) {
      return (y - a) / b;
    },
    a: a,
    b: b,
  };
};

powerLawModel = function (a, b) {
  return {
    y: function (x) {
      return a * Math.pow(x, b);
    },
    x: function (y) {
      return Math.pow(y / a, 1 / b);
    },
    a: a,
    b: b,
  };
};

powerLawRegression = function (points) {
  var X,
    Y,
    a,
    b,
    i,
    logX,
    logY,
    model,
    n,
    p,
    sqSumLogX,
    sumLogX,
    sumLogXLogY,
    sumLogY,
    sumSqLogX;
  n = points.length;
  X = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      p = points[_i];
      _results.push(p[0]);
    }
    return _results;
  })();
  Y = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      p = points[_i];
      _results.push(p[1]);
    }
    return _results;
  })();
  logX = X.map(Math.log);
  logY = Y.map(Math.log);
  sumLogXLogY = sum(
    (function () {
      var _i, _results;
      _results = [];
      for (i = _i = 0; 0 <= n ? _i < n : _i > n; i = 0 <= n ? ++_i : --_i) {
        _results.push(logX[i] * logY[i]);
      }
      return _results;
    })(),
  );
  sumLogX = sum(logX);
  sumLogY = sum(logY);
  sumSqLogX = sum(
    logX.map(function (v) {
      return v * v;
    }),
  );
  sqSumLogX = sumLogX * sumLogX;
  b = (n * sumLogXLogY - sumLogX * sumLogY) / (n * sumSqLogX - sqSumLogX);
  a = (sumLogY - b * sumLogX) / n;
  model = powerLawModel(Math.exp(a), b);
  model.mse = function () {
    return mse(_y, points);
  };
  return model;
};

fixedPointPowerLawRegression = function (points, fixedPoint) {
  /*
      given fixed point: (p, q)
      the model would be: y = q(x/p)^b
      minimize its residual: ln(y) = b * ln(x/p) + ln(q)
        y_i' = b * x_i'
          x_i' = ln(x_i/p)
          y_i' = ln(y_i) - ln(q)
     */
  var X, Y, b, i, logQ, model, n, p, point, q;
  n = points.length;
  p = fixedPoint[0];
  q = fixedPoint[1];
  logQ = Math.log(q);
  X = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      point = points[_i];
      _results.push(Math.log(point[0] / p));
    }
    return _results;
  })();
  Y = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      point = points[_i];
      _results.push(Math.log(point[1]) - logQ);
    }
    return _results;
  })();
  b = linearRegressionThroughOrigin(
    (function () {
      var _i, _results;
      _results = [];
      for (i = _i = 0; 0 <= n ? _i < n : _i > n; i = 0 <= n ? ++_i : --_i) {
        _results.push([X[i], Y[i]]);
      }
      return _results;
    })(),
  ).b;
  model = powerLawModel(q / Math.pow(p, b), b);
  return model;
};

linearRegressionThroughOrigin = function (points) {
  var X, Y, b, i, n, p, sumSqX, sumXY;
  n = points.length;
  X = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      p = points[_i];
      _results.push(p[0]);
    }
    return _results;
  })();
  Y = (function () {
    var _i, _len, _results;
    _results = [];
    for (_i = 0, _len = points.length; _i < _len; _i++) {
      p = points[_i];
      _results.push(p[1]);
    }
    return _results;
  })();
  sumXY = sum(
    (function () {
      var _i, _results;
      _results = [];
      for (i = _i = 0; 0 <= n ? _i < n : _i > n; i = 0 <= n ? ++_i : --_i) {
        _results.push(X[i] * Y[i]);
      }
      return _results;
    })(),
  );
  sumSqX = sum(
    X.map(function (v) {
      return v * v;
    }),
  );
  b = sumXY / sumSqX;
  return {
    y: function (x) {
      return b * x;
    },
    x: function (y) {
      return y / b;
    },
    b: b,
  };
};

export default SM;
// /*
//  * SuperMemo 15
//  * @param {data} is item
// */
// sm = new _this.SM();
// SM = _this.SM;
// export {sm, SM};

// export function add(value) {
//   return sm.addItem(value);
//   return;
// }

// // export function nextAdv(){
// // 	data = sm.nextItem(mode[1] === '_adv');
// // }

// export function answer(score, data){
//   if((0 <= score && score <= 5)){
// 		sm.answer(score, data)
// 	}
// }

// export function discard(data){
// 	sm.discard(data);
// }


// export function save(callback, items){
// 	callback(items)
// }

// export function load(callback){
// 	const data = callback();
// 	_this.sm = _this.SM.load(data);
// }

// export function list(){
// 	var _i, _len, _ref, _results;
// 	_ref = sm.q;
// 	_results = [];
// 	for (_i = 0, _len = _ref.length; _i < _len; _i++) {
// 		item = _ref[_i];
// 		_results.push(JSON.stringify(item.data()));
// 	}
// 	return _results;
// }

// export function exit(items){
//   save(items);
// }
